package de.webdroid.android.websms.connector.mobilant;

import android.os.Bundle;
import de.ub0r.android.websms.connector.common.ConnectorPreferenceActivity;

/**
 * Preferences.
 * 
 * @author Webdroid
 */
public final class Preferences extends ConnectorPreferenceActivity {

	/** Preference key: enabled. */
	static final String PREFS_ENABLED = "enable_mobilant";

	/** Preference's gateway key. */
	static final String PREFS_GATEWAY_KEY = "gateway_key_mobilant";

	/** Preference's lowcost. */
	static final String PREFS_LOWCOST = "lowcost_mobilant";

	/** Preference's plus option. */
	static final String PREFS_PLUS = "plus_mobilant";

	/** Preference's debug modus. */
	static final String PREFS_DEBUG = "debug_mobilant";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.connector_mobilant_prefs);
	}
}
