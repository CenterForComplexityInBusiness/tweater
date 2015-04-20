package edu.umd.cs.dmonner.tweater;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;

import twitter4j.FilterQuery;
import twitter4j.GeoLocation;
import twitter4j.TwitterStream;
import edu.umd.cs.dmonner.tweater.util.Util;

/**
 * The main point of contact with the Twitter Streaming API. It handles connecting to the API, and
 * then sends all pertinent info to every <code>StatusQueue</code> that was registered via the
 * <code>addServer</code> method.
 * 
 * This class periodically asks the supplied <code>QueryBuilder</code> for the current query, and
 * handles re-connecting to the Twitter Streaming API if the query is out of date.
 * 
 * @author dmonner
 */
public class Querier extends Thread
{
	/**
	 * Identifier specifying the log file to use
	 */
	private final String id;
	/**
	 * List of queues to which to send statuses
	 */
	private final List<StatusQueue> queues;
	/**
	 * The connection to Twitter
	 */
	private final TwitterStream tw;
	/**
	 * The collection of track keywords
	 */
	private String[] track;
	/**
	 * The collection of user IDs to follow
	 */
	private long[] follow;
	/**
	 * The collection of locations to track
	 */
	private double[][] location;
	/**
	 * The minimum amount of time (ms) allowed between each request to Twitter for a change in the
	 * query
	 */
	private static final long MIN_INTERVAL = 1 * 60 * 1000;
	/**
	 * Reference to the query builder
	 */
	private final QueryBuilder builder;
	/**
	 * Collection of active query items
	 */
	private TreeSet<QueryItem> active;
	/**
	 * Collection of query items added since the last update; for logging purposes
	 */
	private final TreeSet<QueryItem> added;
	/**
	 * Collection of query items removed since the last update; for logging purposes
	 */
	private final TreeSet<QueryItem> removed;
	/**
	 * The time (in ms since the epoch) when the query was last sent to Twitter
	 */
	private long lastUpdate;
	/**
	 * Whether or not we need to send a new query to Twitter at the next opportunity
	 */
	private boolean needsUpdate;
	/**
	 * Whether or not we are shut down (permanently disconnected for this session)
	 */
	private boolean shutdown;

	private final Logger log;

	public Querier(final String id, final TwitterStream tw, final QueryBuilder qb)
	{
		this.id = id;
		this.queues = new LinkedList<StatusQueue>();

		this.tw = tw;
		this.track = new String[0];
		this.follow = new long[0];
		this.location = new double[0][2];

		this.builder = qb;
		this.active = new TreeSet<QueryItem>();
		this.added = new TreeSet<QueryItem>();
		this.removed = new TreeSet<QueryItem>();

		this.lastUpdate = -MIN_INTERVAL;
		this.needsUpdate = true;
		this.shutdown = false;

		this.log = Logger.getLogger(id);
	}

	/**
	 * Adds a <code>QueryItem</code> to this querier, as well as all associated queues.
	 * 
	 * @param item
	 */
	protected void addItem(final QueryItem item)
	{
		added.add(item);
		for(final StatusQueue server : queues)
			server.addItem(item);
	}

	/**
	 * Adds a new queue which will receive all statuses that this querier receives.
	 * 
	 * @param queue
	 */
	public void addQueue(final StatusQueue queue)
	{
		queues.add(queue);
		tw.addListener(queue);
	}

	/**
	 * Connects to the Twitter Streaming API with the current query. If this querier has been marked
	 * as shut down, this method does nothing.
	 */
	public void connect()
	{
		if(!shutdown && (track.length > 0 || follow.length > 0 || location.length > 0))
		{
			final FilterQuery fq = new FilterQuery();
			fq.track(track);
			fq.follow(follow);
			fq.locations(location);
			log.info("Querier connecting: " + this.toString());
			log.info("+" + added);
			log.info("-" + removed);
			added.clear();
			removed.clear();
			tw.filter(fq);
		}
	}

	/**
	 * Removes a specific <code>QueryItem</code> from this querier, as well as all associated queues.
	 * 
	 * @param item
	 */
	protected void delItem(final QueryItem item)
	{
		removed.add(item);
		for(final StatusQueue server : queues)
			server.delItem(item);
	}

	/**
	 * Disconnects from the Twitter Streaming API.
	 */
	public void disconnect()
	{
		tw.cleanUp();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run()
	{
		// Loop until we are shut down, periodically asking the QueryBuilder if the query has been
		// updated
		while(!shutdown)
		{
			synchronized(this)
			{
				final long now = new Date().getTime();
				final TreeSet<QueryItem> current = builder.at(now);

				log.finest("Queryier.run():");
				log.finest("active query: " + active);
				log.finest("new query: " + current);

				// compare the "current" tree to the "active" tree
				final TreeSet<QueryItem> toAdd = new TreeSet<QueryItem>(current);
				toAdd.removeAll(active);

				final TreeSet<QueryItem> toRemove = new TreeSet<QueryItem>(active);
				toRemove.removeAll(current);

				// if there are differences
				if(!toAdd.isEmpty() || !toRemove.isEmpty())
				{
					// set the flag to disconnect/update query/reconnect
					needsUpdate = true;

					// set the active set to reflect the new current set
					active = current;

					// send the change deltas to all servers
					for(final QueryItem qi : toAdd)
						addItem(qi);

					for(final QueryItem qi : toRemove)
						delItem(qi);
				}

				// if we need to update, and it's been long enough since the last update
				if(needsUpdate && now > lastUpdate + MIN_INTERVAL)
				{
					setQuery(active);
					lastUpdate = now;
					needsUpdate = false;
				}
			}

			// Wait a while before starting the loop again
			try
			{
				Thread.sleep((int) (2000.0 + Math.random() * 1000.0));
			}
			catch(final InterruptedException ex)
			{
				Logger.getLogger(id).severe(Util.traceMessage(ex));
			}
		}

		Logger.getLogger(id).info("Querier shut down.");
	}

	/**
	 * Replaces the current query with one defined by the given collection. Disconnects from Twitter
	 * if necessary and immediately reconnects with the new query.
	 * 
	 * @param items
	 */
	public void setQuery(final Collection<QueryItem> items)
	{
		final List<String> tracks = new LinkedList<String>();
		final List<Long> follows = new LinkedList<Long>();
		final List<GeoLocation> locations = new LinkedList<GeoLocation>();

		for(final QueryItem item : items) {
			if(item instanceof QueryTrack) {
				tracks.add(((QueryTrack) item).string);
			} else if(item instanceof QueryPhrase) {
				tracks.add(((QueryPhrase) item).string);
			} else if(item instanceof QueryFollow) {
				follows.add(((QueryFollow) item).userid);
			} else if(item instanceof QueryLocation) {
				locations.add(((QueryLocation) item).pointSW);
				locations.add(((QueryLocation) item).pointNE);
			}
		}

		track = tracks.toArray(new String[tracks.size()]);
		follow = new long[follows.size()];
		int idx = 0;
		for(long id: follows) {
			follow[idx++] = id;
		}

		location = new double[locations.size()][2];
		idx = 0;
		for(GeoLocation pt: locations) {
			location[idx][0] = pt.getLongitude();
			location[idx][1] = pt.getLatitude();
			idx += 1;
		}
		
		disconnect();
		connect();
	}

	/**
	 * Shuts down this querier, disconnecting it permanently from Twitter.
	 */
	public void shutdown()
	{
		shutdown = true;
		disconnect();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#toString()
	 */
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();

		sb.append("{Track&Phrase=");

		sb.append("[");

		if(track.length > 0)
			sb.append(track[0]);

		for(int i = 1; i < track.length; i++)
		{
			sb.append(", ");
			sb.append(track[i]);
		}

		sb.append("]");

		sb.append(", Follow=");

		sb.append("[");

		if(follow.length > 0)
			sb.append(follow[0]);
		for(int i = 1; i < follow.length; i++)
		{
			sb.append(", ");
			sb.append(follow[i]);
		}

		sb.append("]");

		sb.append(", Location=");

		sb.append("[");

		if(location.length > 0)
			sb.append(location[0]);
		for(int i = 1; i < location.length; i++)
		{
			sb.append(", ");
			sb.append(location[i]);
		}

		sb.append("]");

		sb.append("}");

		return sb.toString();
	}
}
