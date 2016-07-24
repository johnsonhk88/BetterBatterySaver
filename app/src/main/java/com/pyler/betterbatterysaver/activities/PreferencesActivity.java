package com.pyler.betterbatterysaver.activities;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.pyler.betterbatterysaver.BuildConfig;
import com.pyler.betterbatterysaver.R;
import com.pyler.betterbatterysaver.util.Constants;
import com.pyler.betterbatterysaver.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PreferencesActivity extends PreferenceActivity {
    public static Context mContext;
    public static SharedPreferences mPrefs;
    public static Utils mUtils;

    public static boolean isXposedModuleEnabled() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mUtils = new Utils(this);
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();

        /*RootSharedPreferences s = new RootSharedPreferences(mContext, "de.robv.android.xposed.installer");
        s.init();
        Map<String, ?> k = s.getSharedPreferences().getAll();
        for (Map.Entry g : k.entrySet()) {
            Toast.makeText(this, g.getKey() + ": " + g.getValue(), Toast.LENGTH_LONG).show();
        }*/
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new Settings()).commit();
    }

    @SuppressWarnings("deprecation")
    public static class Settings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (Build.VERSION.SDK_INT < 24) {
                getPreferenceManager()
                        .setSharedPreferencesMode(MODE_WORLD_READABLE);
            }
            addPreferencesFromResource(R.xml.preferences);
            mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            PreferenceScreen mainSettings = (PreferenceScreen) findPreference("better_battery_saver");

            // **** App battery saver settings **** //
            Preference showSystemApps = (Preference) findPreference("show_system_apps");
            showSystemApps.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(
                        Preference preference, Object newValue) {
                    reloadAppsList();
                    return true;
                }
            });

            Preference.OnPreferenceChangeListener showSystemAppsListener = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(
                        Preference preference, Object newValue) {
                    mPrefs.edit().putBoolean("show_system_apps", (boolean) newValue).apply();
                    reloadAppsList();
                    return true;
                }
            };

            Preference showSystemServices = (Preference) findPreference("show_system_services");
            if (showSystemServices != null) {
                showSystemServices.setOnPreferenceChangeListener(showSystemAppsListener);
            }

            Preference showSystemReceivers = (Preference) findPreference("show_system_receivers");
            if (showSystemReceivers != null) {
                showSystemReceivers.setOnPreferenceChangeListener(showSystemAppsListener);
            }

            // **** Battery battery saver settings **** //
            final PreferenceScreen appBatterySavingSettings = (PreferenceScreen) findPreference("app_battery_saving_settings");

            if (!isXposedModuleEnabled()) {
                mainSettings.removePreference(appBatterySavingSettings);
            }

            final PreferenceCategory batterySaverOn = (PreferenceCategory) findPreference("battery_saver_on");
            final PreferenceCategory batterySaverOff = (PreferenceCategory) findPreference("battery_saver_off");
            PreferenceScreen appServiceManager = (PreferenceScreen) findPreference("app_services_manager");
            PreferenceScreen appReceiverManager = (PreferenceScreen) findPreference("app_receivers_manager");
            PreferenceScreen settings = (PreferenceScreen) findPreference("settings");

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                batterySaverOn.removePreference(findPreference("turn_android_saver_off"));
                batterySaverOff.removePreference(findPreference("turn_android_saver_on"));
                settings.removePreference(findPreference("headsup_notifications"));
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                batterySaverOn.removePreference(findPreference("turn_doze_off"));
                batterySaverOff.removePreference(findPreference("turn_doze_on"));
            }

            if (!mUtils.hasRoot()) {
                batterySaverOn.removePreference(findPreference("turn_device_off"));
                batterySaverOn.removePreference(findPreference("turn_screen_off"));
                batterySaverOn.removePreference(findPreference("turn_mobile_data_off"));
                batterySaverOn.removePreference(findPreference("turn_airplane_mode_on"));
                batterySaverOn.removePreference(findPreference("turn_nfc_off"));
                batterySaverOn.removePreference(findPreference("turn_gps_off"));

                batterySaverOff.removePreference(findPreference("turn_screen_on"));
                batterySaverOff.removePreference(findPreference("turn_mobile_data_on"));
                batterySaverOff.removePreference(findPreference("turn_airplane_mode_off"));
                batterySaverOff.removePreference(findPreference("turn_nfc_on"));
                batterySaverOff.removePreference(findPreference("turn_gps_on"));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    batterySaverOn.removePreference(findPreference("turn_android_saver_on"));
                    batterySaverOff.removePreference(findPreference("turn_android_saver_off"));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    batterySaverOn.removePreference(findPreference("turn_doze_on"));
                    batterySaverOff.removePreference(findPreference("turn_doze_off"));
                }

                mainSettings.removePreference(appServiceManager);
                mainSettings.removePreference(appReceiverManager);
            }

            reloadAppsList();


            Preference batteryLevelThreshold = (Preference) findPreference("battery_level_threshold");
            int batteryLevel = mUtils.getBatteryLevelThreshold();
            String batteryLevelThresholdTitle = getString(R.string.battery_level_threshold, batteryLevel);
            batteryLevelThreshold.setTitle(batteryLevelThresholdTitle);
            batteryLevelThreshold.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(
                        Preference preference, Object newValue) {
                    int batteryLevel = (int) newValue;
                    String title = getString(R.string.battery_level_threshold, batteryLevel);
                    preference.setTitle(title);
                    return true;
                }
            });

            boolean setScreenTimeoutOffValue = mUtils.getBooleanPreference("set_screen_timeout_off");
            final Preference screenTimeoutOff = (Preference) findPreference("screen_timeout_off");
            int screenTimeoutOffValue = mUtils.getIntPreference("screen_timeout_off");
            String screenTimeoutOffTitle = getString(R.string.screen_timeout, screenTimeoutOffValue);
            screenTimeoutOff.setTitle(screenTimeoutOffTitle);
            screenTimeoutOff.setEnabled(setScreenTimeoutOffValue);


            boolean setScreenTimeoutOnValue = mUtils.getBooleanPreference("set_screen_timeout_on");
            final Preference screenTimeoutOn = (Preference) findPreference("screen_timeout_on");
            int screenTimeoutOnValue = mUtils.getIntPreference("screen_timeout_on");
            String screenTimeoutOnTitle = getString(R.string.screen_timeout, screenTimeoutOnValue);
            screenTimeoutOn.setEnabled(setScreenTimeoutOnValue);
            screenTimeoutOn.setTitle(screenTimeoutOnTitle);

            Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(
                        Preference preference, Object newValue) {
                    int screenTimeout = (int) newValue;
                    String title = getString(R.string.screen_timeout, screenTimeout);
                    preference.setTitle(title);
                    return true;
                }
            };

            screenTimeoutOff.setOnPreferenceChangeListener(listener);
            screenTimeoutOn.setOnPreferenceChangeListener(listener);

            // **** App batery saving settings **** //

            Preference useAppBatterySaving = (Preference) findPreference("app_battery_saving");
            if (useAppBatterySaving != null) {
                final PreferenceCategory appSettings = (PreferenceCategory) findPreference("app_settings");
                boolean useAppBatterySavingValue = mUtils.getBooleanPreference("app_battery_saving");
                if (useAppBatterySavingValue) {
                    appBatterySavingSettings.addPreference(appSettings);
                    reloadAppsList();
                } else {
                    appBatterySavingSettings.removePreference(appSettings);

                }

                useAppBatterySaving.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(
                            Preference preference, Object newValue) {
                        boolean set = (boolean) newValue;
                        if (set) {
                            appBatterySavingSettings.addPreference(appSettings);
                        } else {
                            appBatterySavingSettings.removePreference(appSettings);

                        }
                        return true;
                    }
                });
            }

            // **** Settings **** //
            Preference resetAllSettings = (Preference) findPreference("reset_all_settings");
            resetAllSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()

                                                          {

                                                              @Override
                                                              public boolean onPreferenceClick(Preference preference) {
                                                                  mPrefs.edit().clear().apply();
                                                                  getActivity().recreate();
                                                                  return false;
                                                              }
                                                          }

            );
            // **** About **** //
            Preference version = (Preference) findPreference("version");
            version.setSummary(BuildConfig.VERSION_NAME);

            Preference support = (Preference) findPreference("support");
            support.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()

                                                 {

                                                     @Override
                                                     public boolean onPreferenceClick(Preference preference) {
                                                         openLink("http://forum.xda-developers.com/xposed/modules/mod-battery-saver-t3419064");
                                                         return false;
                                                     }
                                                 }

            );

            Preference donate = (Preference) findPreference("donate");
            donate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()

                                                {

                                                    @Override
                                                    public boolean onPreferenceClick(Preference preference) {
                                                        openLink("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=6NTYA2HMPQHVW");
                                                        return false;
                                                    }
                                                }

            );


        }

        private void openLink(String url) {
            Intent openUrl = new Intent(Intent.ACTION_VIEW);
            openUrl.setData(Uri.parse(url));
            openUrl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(openUrl);
            } catch (ActivityNotFoundException e) {
                // nothing
            }
        }

        @TargetApi(Build.VERSION_CODES.M)
        private void openManageWriteSettings() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
            AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
            dialog.setTitle(R.string.modify_system_settings);
            dialog.setMessage(R.string.grant_write_system_settings_permission_message);
            dialog.setCancelable(true);

            dialog.setPositiveButton(
                    R.string.grant_write_system_settings_permission,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            Intent writeSystemSettings = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                            writeSystemSettings.setData(Uri.parse("package:" + Constants.PACKAGE_NAME));
                            writeSystemSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            try {
                                startActivity(writeSystemSettings);
                            } catch (ActivityNotFoundException e) {
                                // nothing
                            }
                        }
                    });
            AlertDialog alertDialog = dialog.create();
            alertDialog.show();
        }

        @Override
        public void onResume() {
            super.onResume();

            Preference brightnessOff = (Preference) findPreference("turn_brightness_off");
            Preference brightnessOn = (Preference) findPreference("turn_brightness_on");
            Preference setScreenTimeoutOff = (Preference) findPreference("set_screen_timeout_off");
            Preference setScreenTimeoutOn = (Preference) findPreference("set_screen_timeout_on");
            Preference wifiApOff = (Preference) findPreference("turn_wifi_ap_off");
            Preference wifiApOn = (Preference) findPreference("turn_wifi_ap_on");

            if (!mUtils.canWriteSystemSettings()) {
                Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(
                            Preference preference, Object newValue) {
                        openManageWriteSettings();
                        return false;
                    }
                };
                brightnessOff.setOnPreferenceChangeListener(listener);
                brightnessOn.setOnPreferenceChangeListener(listener);
                setScreenTimeoutOff.setOnPreferenceChangeListener(listener);
                setScreenTimeoutOn.setOnPreferenceChangeListener(listener);
                wifiApOff.setOnPreferenceChangeListener(listener);
                wifiApOn.setOnPreferenceChangeListener(listener);
            } else {
                brightnessOff.setOnPreferenceChangeListener(null);
                brightnessOn.setOnPreferenceChangeListener(null);
                wifiApOff.setOnPreferenceChangeListener(null);
                wifiApOn.setOnPreferenceChangeListener(null);
                final Preference screenTimeoutOff = (Preference) findPreference("screen_timeout_off");
                final Preference screenTimeoutOn = (Preference) findPreference("screen_timeout_on");
                setScreenTimeoutOff.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(
                            Preference preference, Object newValue) {
                        boolean set = (boolean) newValue;
                        screenTimeoutOff.setEnabled(set);
                        return true;
                    }
                });

                setScreenTimeoutOn.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(
                            Preference preference, Object newValue) {
                        boolean set = (boolean) newValue;
                        screenTimeoutOn.setEnabled(set);
                        return true;
                    }
                });
            }
        }

        @Override
        public void onPause() {
            super.onPause();

            mUtils.setPrefsFileWorldReadable();
        }

        public void reloadAppsList() {
            new LoadApps().execute();
        }

        public boolean isAllowedApp(ApplicationInfo appInfo) {
            boolean showSystemApps = mUtils.getBooleanPreference("show_system_apps");
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    && !showSystemApps) {
                return false;
            }
            return true;
        }

        public class LoadApps extends AsyncTask<Void, Void, Void> {
            PreferenceCategory appSettings = (PreferenceCategory) findPreference("app_settings");
            PreferenceCategory appServiceSettings = (PreferenceCategory) findPreference("app_service_settings");
            PreferenceCategory appReceiverSettings = (PreferenceCategory) findPreference("app_receiver_settings");

            PackageManager pm = mContext.getPackageManager();
            List<ApplicationInfo> packages = pm
                    .getInstalledApplications(PackageManager.GET_META_DATA);

            @Override
            protected Void doInBackground(Void... arg0) {
                List<String[]> sortedApps = new ArrayList<>();
                if (appSettings != null) appSettings.removeAll();
                if (appServiceSettings != null) appServiceSettings.removeAll();
                if (appReceiverSettings != null) appReceiverSettings.removeAll();
                for (ApplicationInfo app : packages) {
                    if (isAllowedApp(app)) {
                        sortedApps.add(new String[]{
                                app.packageName,
                                app.loadLabel(mContext.getPackageManager())
                                        .toString()});
                    }
                }

                Collections.sort(sortedApps, new Comparator<String[]>() {
                    @Override
                    public int compare(String[] entry1, String[] entry2) {
                        return entry1[1].compareToIgnoreCase(entry2[1]);
                    }
                });

                for (int i = 0; i < sortedApps.size(); i++) {
                    final String appName = sortedApps.get(i)[1];
                    final String packageName = sortedApps.get(i)[0];
                    Preference appPreference = new Preference(mContext);
                    Preference servicePreference = new Preference(mContext);
                    Preference receiverPreference = new Preference(mContext);
                    appPreference.setTitle(appName);
                    servicePreference.setTitle(appName);
                    receiverPreference.setTitle(appName);
                    appPreference.setSummary(packageName);
                    servicePreference.setSummary(packageName);
                    receiverPreference.setSummary(packageName);
                    appPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Intent openAppSettings = new Intent(mContext, AppSettingsActivity.class);
                            openAppSettings.putExtra("package", packageName);
                            openAppSettings.putExtra("app", appName);
                            startActivity(openAppSettings);
                            return false;
                        }
                    });

                    if (appSettings != null) appSettings.addPreference(appPreference);

                    servicePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Intent openAppSettings = new Intent(mContext, AppServiceSettingsActivity.class);
                            openAppSettings.putExtra("package", packageName);
                            openAppSettings.putExtra("app", appName);
                            startActivity(openAppSettings);
                            return false;
                        }
                    });

                    if (appServiceSettings != null)
                        appServiceSettings.addPreference(servicePreference);

                    receiverPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Intent openAppSettings = new Intent(mContext, AppReceiverSettingsActivity.class);
                            openAppSettings.putExtra("package", packageName);
                            openAppSettings.putExtra("app", appName);
                            startActivity(openAppSettings);
                            return false;
                        }
                    });

                    if (appReceiverSettings != null)
                        appReceiverSettings.addPreference(receiverPreference);
                }

                return null;
            }
        }

    }
}
