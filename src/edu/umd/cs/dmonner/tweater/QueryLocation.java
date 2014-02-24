package edu.umd.cs.dmonner.tweater;

import twitter4j.GeoLocation;

/**
 * A <code>QueryItem</code> that matches tweets on a latitude/longitude bounding box.
 * 
 * @author rmachedo
 */
public class QueryLocation extends QueryItem {
	private static final long serialVersionUID = 1L;

	/**
	 * Coordinates of the bounding-box for the location being searched for
	 */
	public final GeoLocation pointSW;
	public final GeoLocation pointNE;

	/**
	 * Creates a new <code>QueryLocation</code> with the given group number, unique
	 * ID, and the location bounding-box that we wish to find defined by it south-west
	 * and north-east longitude/latitude points.
	 * 
	 * @param group
	 * @param id
	 * @param pointSW
	 * @param pointNE
	 * 
	 * @throws IllegalArgumentException If the south-west point is not to the south and west 
	 * of the north-east point, or if the points span outside of <code>[-90, 90]</code> on 
	 * latitude or <code>[-180, 180]</code> on longitude.
	 */
	public QueryLocation(final int group, final int id, GeoLocation pointSW, GeoLocation pointNE) {
		super(Type.LOCATION, group, id);
		
		if(pointSW.getLatitude() >= pointNE.getLatitude() || pointSW.getLongitude() >= pointNE.getLongitude())
		{
			throw new IllegalArgumentException(String.format("South-west point was not to the south and west of north-east point: (%s, %s) - Twitter will not like this!", pointSW, pointNE));
		} else if(pointSW.getLatitude() > 90 || pointSW.getLatitude() < -90)
		{
			throw new IllegalArgumentException(String.format("South-west point exceeded [-90, 90] latitude bounds: %s - Twitter will not like this!", pointSW));
		} else if(pointSW.getLongitude() > 180 || pointSW.getLongitude() < -180)
		{
			throw new IllegalArgumentException(String.format("South-west point exceeded [-180, 180] longitude bounds: %s - Twitter will not like this!", pointSW));
		} else if(pointNE.getLatitude() > 90 || pointNE.getLatitude() < -90)
		{
			throw new IllegalArgumentException(String.format("North-east point exceeded [-90, 90] latitude bounds: %s - Twitter will not like this!", pointNE));
		} else if(pointNE.getLongitude() > 180 || pointNE.getLongitude() < -180)
		{
			throw new IllegalArgumentException(String.format("North-east point exceeded [-180, 180] longitude bounds: %s", pointNE));
		}
		
		this.pointNE = pointNE;
		this.pointSW = pointSW;
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

		GeoLocation otherSW = ((QueryLocation) that).pointSW;
		GeoLocation otherNE = ((QueryLocation) that).pointNE;
		
		if(pointSW.getLongitude() > otherSW.getLongitude())
			return 1;
		else if(pointSW.getLongitude() < otherSW.getLongitude())
			return -1;
		if(pointNE.getLongitude() > otherNE.getLongitude())
			return 1;
		else if(pointNE.getLongitude() < otherNE.getLongitude())
			return -1;
		if(pointSW.getLatitude() > otherSW.getLatitude())
			return 1;
		else if(pointSW.getLatitude() < otherSW.getLatitude())
			return -1;
		if(pointNE.getLatitude() > otherNE.getLatitude())
			return 1;
		else if(pointNE.getLatitude() < otherNE.getLatitude())
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

		return this.pointSW.equals(((QueryLocation) other).pointSW) && this.pointNE.equals(((QueryLocation) other).pointNE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tweater.QueryItem#hashCode()
	 */
	@Override
	public int hashCode() {
		return super.hashCode() + this.pointSW.hashCode() + this.pointNE.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tweater.QueryItem#matches(tweater.MatchableStatus)
	 */
	@Override
	public boolean matches(final MatchableStatus status) {
		GeoLocation l;
		if((l = status.status.getGeoLocation()) != null) {
			return l.getLatitude() >= pointSW.getLatitude() &&
					l.getLatitude() <= pointNE.getLatitude() &&
					l.getLongitude() >= pointSW.getLongitude() &&
					l.getLongitude() < pointNE.getLongitude();
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
		return String.format("(%s, %s)", pointSW.toString(), pointNE.toString());
	}
}
