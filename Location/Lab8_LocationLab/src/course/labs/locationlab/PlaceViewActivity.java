package course.labs.locationlab;

import android.app.ListActivity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class PlaceViewActivity extends ListActivity implements LocationListener {
	private static final long FIVE_MINS = 5 * 60 * 1000;
	private static final String TAG = "Lab-Location";

	// False if you don't have network access
	public static boolean sHasNetwork = true;

	private Location mLastLocationReading;
	private PlaceViewAdapter mAdapter;
	private LocationManager mLocationManager;
	private boolean mMockLocationOn = false;

	// default minimum time between new readings
	private long mMinTime = 5000;

	// default minimum distance between old and new readings.
	private float mMinDistance = 1000.0f;

	// A fake location provider used for testing
	private MockLocationProvider mMockLocationProvider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up the app's user interface. This class is a ListActivity,
		// so it has its own ListView. ListView's adapter should be a PlaceViewAdapter

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		ListView placesListView = getListView();

		View footerView = getLayoutInflater().inflate(R.layout.footer_view, null);

		footerView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				//There is no current location
				if (mLastLocationReading == null) {
					//					footerView.setClickable(false);
					Toast.makeText(
							PlaceViewActivity.this,
							R.string.location_not_identified_yet,
							Toast.LENGTH_SHORT)
							.show();

					return;
				}

				// There is a current location, but the user has already acquired a
				// PlaceBadge for this location
				if (mAdapter != null && mAdapter.intersects(mLastLocationReading)) {

					Toast.makeText(
							PlaceViewActivity.this,
							R.string.duplicate_location_string,
							Toast.LENGTH_SHORT)
							.show();

					return;
				}

				// There is a current location for which the user does not already have
				// a PlaceBadge.
				PlaceDownloaderTask task =
						new PlaceDownloaderTask(PlaceViewActivity.this, sHasNetwork);
				task.execute(mLastLocationReading);
			}

		});

		placesListView.addFooterView(footerView);
		mAdapter = new PlaceViewAdapter(getApplicationContext());
		setListAdapter(mAdapter);

	}

	@Override
	protected void onResume() {
		super.onResume();

		startMockLocationManager();

		mLastLocationReading =
				mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		if(mLastLocationReading != null && ageInMilliseconds(mLastLocationReading) > FIVE_MINS)
			mLastLocationReading = null;

		// TODA - register to receive location updates from NETWORK_PROVIDER
		if(mLocationManager.getProvider(LocationManager.NETWORK_PROVIDER) != null) {
			mLocationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, mMinTime, mMinDistance, this);
		}
	}

	@Override
	protected void onPause() {

		mLocationManager.removeUpdates(this);

		shutdownMockLocationManager();
		super.onPause();
	}

	// Callback method used by PlaceDownloaderTask
	public void addNewPlace(PlaceRecord place) {

		mAdapter.add(place);
	}

	// LocationListener methods
	@Override
	public void onLocationChanged(Location currentLocation) {

		if(mLastLocationReading == null) {
			mLastLocationReading = currentLocation;

			return;
		}


		if(mLastLocationReading.getTime() < currentLocation.getTime()) {
			mLastLocationReading = currentLocation;
			return;
		}

		mLastLocationReading = null;
	}

	@Override
	public void onProviderDisabled(String provider) {
		// not implemented
	}

	@Override
	public void onProviderEnabled(String provider) {
		// not implemented
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// not implemented
	}

	// Returns age of location in milliseconds
	private long ageInMilliseconds(Location location) {
		return System.currentTimeMillis() - location.getTime();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.delete_badges:
				mAdapter.removeAllViews();
				return true;
			case R.id.place_one:
				mMockLocationProvider.pushLocation(37.422, -122.084);
				return true;
			case R.id.place_no_country:
				mMockLocationProvider.pushLocation(0, 0);
				return true;
			case R.id.place_two:
				mMockLocationProvider.pushLocation(38.996667, -76.9275);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void shutdownMockLocationManager() {
		if (mMockLocationOn) {
			mMockLocationProvider.shutdown();
		}
	}

	private void startMockLocationManager() {
		if (!mMockLocationOn) {
			mMockLocationProvider = new MockLocationProvider(
					LocationManager.NETWORK_PROVIDER, this);
		}
	}
}
