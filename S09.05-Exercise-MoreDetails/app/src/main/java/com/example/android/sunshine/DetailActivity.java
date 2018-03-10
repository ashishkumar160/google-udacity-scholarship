/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.utilities.SunshineDateUtils;
import com.example.android.sunshine.utilities.SunshineWeatherUtils;

import org.w3c.dom.Text;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    /*
     * In this Activity, you can share the selected day's forecast. No social sharing is complete
     * without using a hashtag. #BeTogetherNotTheSame
     */
    private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";

    String[] cols = {
        WeatherContract.WeatherEntry.COLUMN_DATE,
        WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
        WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
        WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
        WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
        WeatherContract.WeatherEntry.COLUMN_PRESSURE,
        WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_DEGREES
    };

    public static final int INDEX_DATE = 0;
    public static final int INDEX_ID = 1;
    public static final int INDEX_MIN = 2;
    public static final int INDEX_MAX = 3;
    public static final int INDEX_WIND = 4;
    public static final int INDEX_PRESSURE = 5;
    public static final int INDEX_HUMIDITY = 6;
    public static final int INDEX_DEGREES = 7;

    public static final int LOADER_ID = 45;

    /* A summary of the forecast that can be shared by clicking the share button in the ActionBar */
    private String mForecastSummary;

    private Uri mUri;

    private TextView dateTextView, descriptionTextView, highTextView, lowTextView, humidityTextView,
                windTextView, pressureTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        dateTextView = (TextView) findViewById(R.id.day_textView);
        descriptionTextView = (TextView) findViewById(R.id.description_textView);
        highTextView = (TextView) findViewById(R.id.high_temp_textView);
        lowTextView = (TextView) findViewById(R.id.low_temp_textView);
        humidityTextView = (TextView) findViewById(R.id.humidity_textView);
        windTextView = (TextView) findViewById(R.id.wind_textView);
        pressureTextView = (TextView) findViewById(R.id.pressure_textView);
        Intent intentThatStartedThisActivity = getIntent();
        if (intentThatStartedThisActivity != null) {
            mUri = intentThatStartedThisActivity.getData();
            if (mUri == null)
                throw new NullPointerException("Uri was null");
        }

        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    /**
     * This is where we inflate and set up the menu for this Activity.
     *
     * @param menu The options menu in which you place your items.
     *
     * @return You must return true for the menu to be displayed;
     *         if you return false it will not be shown.
     *
     * @see #onPrepareOptionsMenu
     * @see #onOptionsItemSelected
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Use AppCompatActivity's method getMenuInflater to get a handle on the menu inflater */
        MenuInflater inflater = getMenuInflater();
        /* Use the inflater's inflate method to inflate our menu layout to this menu */
        inflater.inflate(R.menu.detail, menu);
        /* Return true so that the menu is displayed in the Toolbar */
        return true;
    }

    /**
     * Callback invoked when a menu item was selected from this Activity's menu. Android will
     * automatically handle clicks on the "up" button for us so long as we have specified
     * DetailActivity's parent Activity in the AndroidManifest.
     *
     * @param item The menu item that was selected by the user
     *
     * @return true if you handle the menu click here, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Get the ID of the clicked item */
        int id = item.getItemId();

        /* Settings menu item clicked */
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        /* Share menu item clicked */
        if (id == R.id.action_share) {
            Intent shareIntent = createShareForecastIntent();
            startActivity(shareIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Uses the ShareCompat Intent builder to create our Forecast intent for sharing.  All we need
     * to do is set the type, text and the NEW_DOCUMENT flag so it treats our share as a new task.
     * See: http://developer.android.com/guide/components/tasks-and-back-stack.html for more info.
     *
     * @return the Intent to use to share our weather forecast
     */
    private Intent createShareForecastIntent() {
        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .setType("text/plain")
                .setText(mForecastSummary + FORECAST_SHARE_HASHTAG)
                .getIntent();
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        return shareIntent;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID:
                return new CursorLoader(this,
                        mUri,
                        cols,
                        null,
                        null,
                        null);
            default:
                throw new RuntimeException("Loader not implemented : " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        boolean cursorHasValidData = false;
        if (data!=null && data.moveToFirst()) {
            cursorHasValidData = true;
        }
        if (!cursorHasValidData) {
            return;
        }
        long localDateMidnightGmt = data.getLong(INDEX_DATE);
        String dateText = SunshineDateUtils.getFriendlyDateString(this,
                localDateMidnightGmt, true);
        dateTextView.setText(dateText);
        int weatherID = data.getInt(INDEX_ID);
        String description = SunshineWeatherUtils.getStringForWeatherCondition(this,
                weatherID);
        descriptionTextView.setText(description);
        double highInCelsius = data.getDouble(INDEX_MAX);
        String highString = SunshineWeatherUtils.formatTemperature(this,
                highInCelsius);
        highTextView.setText(highString);
        double lowInCelsius = data.getDouble(INDEX_MIN);
        String lowString = SunshineWeatherUtils.formatTemperature(this,
                lowInCelsius);
        lowTextView.setText(lowString);
        float humidity = data.getFloat(INDEX_HUMIDITY);
        String humidityString = getString(R.string.format_humidity, humidity);
        humidityTextView.setText(humidityString);
        float windSpeed = data.getFloat(INDEX_WIND);
        float windDirection = data.getFloat(INDEX_DEGREES);
        String windString = SunshineWeatherUtils.getFormattedWind(this, windSpeed,
                windDirection);
        windTextView.setText(windString);
        float pressure = data.getFloat(INDEX_PRESSURE);
        String pressureString = getString(R.string.format_pressure, pressure);
        pressureTextView.setText(pressureString);
        mForecastSummary = String.format("%s - %s - %s%s", dateText, description, highString, lowString);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }


}