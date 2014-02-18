package edu.umd.cs.dmonner.tweater;

import java.awt.geom.Rectangle2D;

/**
 * A <code>QueryItem</code> that matches on set of keywords, which need not
 * necessarily be in order or adjacent, specified as a single
 * whitespace-separated <code>String</code>.
 * 
 * @author dmonner
 */
public class QueryLocation extends QueryItem {
	private static final long serialVersionUID = 1L;

	/**
	 * Coordinates of the bounding-box for the location being searched for
	 */
	public final Rectangle2D location;

	/**
	 * Creates a new <code>QueryLocation</code> with the given group number, unique
	 * ID, and the location bounding-box that we wish to find.
	 * 
	 * @param group
	 * @param id
	 * @param location
	 */
	public QueryLocation(final int group, final int id, Rectangle2D location) {
		super(Type.LOCATION, group, id);
		this.location = location;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tweater.QueryItem#compareTo(tweater.QueryItem)
	 */
	@Override
	public int compareTo(final QueryItem that) {
		int result;

		result = super.compareTo(that);
		if (result != 0)
			return result;

		Rectangle2D other = ((QueryLocation) that).location;
		
		if(location.getMaxX() > other.getMaxX())
			return 1;
		else if(location.getMaxX() < other.getMaxX())
			return -1;
		if(location.getMinX() > other.getMinX())
			return 1;
		else if(location.getMinX() < other.getMinX())
			return -1;
		if(location.getMaxY() > other.getMaxY())
			return 1;
		else if(location.getMaxY() < other.getMaxY())
			return -1;
		if(location.getMinY() > other.getMinY())
			return 1;
		else if(location.getMinY() < other.getMinY())
			return -1;
		else return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tweater.QueryItem#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object other) {
		boolean result;

		result = super.equals(other);
		if (!result)
			return result;

		return this.location.equals(((QueryLocation) other).location);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tweater.QueryItem#hashCode()
	 */
	@Override
	public int hashCode() {
		return super.hashCode() + location.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tweater.QueryItem#matches(tweater.MatchableStatus)
	 */
	@Override
	public boolean matches(final MatchableStatus status) {
		if(status.status.getGeoLocation() != null) {
			return location.contains(status.status.getGeoLocation().getLongitude(), 
					status.status.getGeoLocation().getLatitude());
		} else {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return location.toString();
	}
}
