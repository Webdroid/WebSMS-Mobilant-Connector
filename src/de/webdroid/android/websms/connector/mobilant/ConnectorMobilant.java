package de.webdroid.android.websms.connector.mobilant;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;

/**
 * Receives commands coming as broadcast from WebSMS.
 * 
 * @author Webdroid
 */
public class ConnectorMobilant extends Connector {

	/** Tag for output. */
	private static final String TAG = "mobilant";

	/** Gateway URL. */
	private static final String URL_SEND = "https://gw.mobilant.net/";

	/** Budget **/
	private static final String URL_BALANCE = URL_SEND + "credits/";

	/** Gateway Cert footprint. */
	private static final String[] CERT_FOOTPRINT = { "62:18:87:f1:ad:3e:82:2c:35:60:0a:18:7a:ee:ed:73:78:77:6e:be" };

	/** Use HTTP POST. */
	private static final boolean USE_POST = false;

	/** Debug. */
	private static final String PARAM_DEBUG = "debug";

	/** Key. */
	private static final String PARAM_KEY = "key";

	/** Message. */
	private static final String PARAM_MESSAGE = "message";

	/** Concat. */
	private static final String PARAM_CONCAT = "concat";

	/** Reciepient. */
	private static final String PARAM_TO = "to";

	/** Sender. */
	private static final String PARAM_SENDER = "from";

	/** Route. */
	private static final String PARAM_ROUTE = "route";

	/** Lowcost. */
	private static final String PARAM_LOWCOST = "lowcost";

	/** Lowcost Plus. */
	private static final String PARAM_LOWCOST_PLUS = "lowcostplus";

	/** Direct. */
	private static final String PARAM_DIRECT = "direct";

	/** Direct Plus. */
	private static final String PARAM_DIRECT_PLUS = "directplus";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec initSpec(final Context context) {
		final String name = context.getString(R.string.connector_mobilant_name);
		ConnectorSpec c = new ConnectorSpec(name);
		c.setAuthor(// .
		context.getString(R.string.connector_mobilant_author));
		c.setBalance(null);
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);

		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);

		if (p.getBoolean(Preferences.PREFS_PLUS, false)) {
			c.addSubConnector(TAG, c.getName(),
					SubConnectorSpec.FEATURE_CUSTOMSENDER);
		}

		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final ConnectorSpec updateSpec(final Context context,
			final ConnectorSpec connectorSpec) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (p.getBoolean(Preferences.PREFS_ENABLED, false)) {
			connectorSpec.setReady();
		} else {
			connectorSpec.setStatus(ConnectorSpec.STATUS_INACTIVE);
		}
		return connectorSpec;
	}

	/**
	 * Check return code from mobilant.de.
	 * 
	 * @param context
	 *            Context
	 * @param ret
	 *            return code
	 * @return true if no error code
	 */
	private static boolean checkReturnCode(final Context context, final int ret) {
		switch (ret) {

		case 100:
			return true;
		case 10:
			throw new WebSMSException(context,
					R.string.response_code_mobilant_010);
		case 20:
			throw new WebSMSException(context,
					R.string.response_code_mobilant_020);
		case 30:
			throw new WebSMSException(context,
					R.string.response_code_mobilant_030);
		case 31:
			throw new WebSMSException(context,
					R.string.response_code_mobilant_031);
		case 32:
			throw new WebSMSException(context,
					R.string.response_code_mobilant_032);
		case 40:
			throw new WebSMSException(context,
					R.string.response_code_mobilant_040);
		case 50:
			throw new WebSMSException(context,
					R.string.response_code_mobilant_050);
		case 60:
			throw new WebSMSException(context,
					R.string.response_code_mobilant_060);
		case 70:
			throw new WebSMSException(context,
					R.string.response_code_mobilant_070);
		case 71:
			throw new WebSMSException(context,
					R.string.response_code_mobilant_071);
		case 80:
			throw new WebSMSException(context,
					R.string.response_code_mobilant_080);
		default:
			throw new WebSMSException(context, R.string.error, " code: " + ret);
		}
	}

	private void sendData(final Context context, final ConnectorCommand command) {

		try {
			final ConnectorSpec cs = this.getSpec(context);
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);

			String url;
			ArrayList<BasicNameValuePair> d = new ArrayList<BasicNameValuePair>();
			final String message = command.getText();

			if (message != null && message.length() > 0) {

				url = URL_SEND;

				if (p.getBoolean(Preferences.PREFS_DEBUG, false)) {
					d.add(new BasicNameValuePair(PARAM_DEBUG, "1"));
				}

				d.add(new BasicNameValuePair(PARAM_TO, Utils
						.joinRecipientsNumbers(command.getRecipients(), ";",
								true)));

				d.add(new BasicNameValuePair(PARAM_MESSAGE, message));

				Log.d(TAG, "LEN: " + message.length());

				if (message.length() > 160) {

					if (message.length() > 1530) {
						throw new WebSMSException(context,
								R.string.response_code_mobilant_032);
					} else {

						d.add(new BasicNameValuePair(PARAM_CONCAT, "1"));
					}
				}

				final String customSender = command.getCustomSender();
				if (customSender == null) {
					d.add(new BasicNameValuePair(PARAM_SENDER, Utils
							.national2international(
									command.getDefPrefix(),
									Utils.getSender(context,
											command.getDefSender()))));
				} else {
					d.add(new BasicNameValuePair(PARAM_SENDER, Utils
							.national2international(command.getDefPrefix(),
									customSender).replaceAll("[^0-9+]", "")));
				}

				Boolean isLowcost = p.getBoolean(Preferences.PREFS_LOWCOST,
						false);
				Boolean isPlus = p.getBoolean(Preferences.PREFS_PLUS, false);

				if (isLowcost) {

					if (isPlus) {
						d.add(new BasicNameValuePair(PARAM_ROUTE,
								PARAM_LOWCOST_PLUS));
					} else {
						d.add(new BasicNameValuePair(PARAM_ROUTE, PARAM_LOWCOST));
					}
				} else {

					if (isPlus) {
						d.add(new BasicNameValuePair(PARAM_ROUTE,
								PARAM_DIRECT_PLUS));
					} else {
						d.add(new BasicNameValuePair(PARAM_ROUTE, PARAM_DIRECT));
					}
				}

			} else {

				url = URL_BALANCE;
			}

			String gateway_key = p.getString(Preferences.PREFS_GATEWAY_KEY, "");

			if (gateway_key.equals("")) {

				throw new WebSMSException(context,
						R.string.connector_mobilant_no_gateway_key);
			}

			d.add(new BasicNameValuePair(PARAM_KEY, gateway_key));

			if (!USE_POST) {
				StringBuilder u = new StringBuilder(url);
				u.append("?");
				final int l = d.size();
				for (int i = 0; i < l; i++) {
					BasicNameValuePair nv = d.get(i);
					u.append(nv.getName());
					u.append("=");
					u.append(URLEncoder.encode(nv.getValue(), "ISO-8859-15"));
					u.append("&");
				}
				url = u.toString();
				d = null;
			}

			Log.d(TAG, "URL: " + url);

			HttpResponse response = Utils.getHttpClient(url, null, d, null,
					null, "ISO-8859-15", CERT_FOOTPRINT);

			int resp = response.getStatusLine().getStatusCode();

			if (resp != HttpURLConnection.HTTP_OK) {
				throw new WebSMSException(context, R.string.error_http, " "
						+ resp);
			}

			String htmlText = Utils.stream2str(
					response.getEntity().getContent()).trim();

			Log.d(TAG, "HTTP RESPONSE: " + htmlText);

			int i = htmlText.indexOf('.');
			if (i > 0) {
				cs.setBalance(htmlText.replace('.', ',') + "\u20AC");
			} else {
				int ret;
				try {
					ret = Integer.parseInt(htmlText.trim());
				} catch (NumberFormatException e) {
					Log.e(TAG, "could not parse text to int: " + htmlText);
					ret = 700;
				}
				checkReturnCode(context, ret);
			}

		} catch (IOException e) {
			Log.e(TAG, null, e);
			throw new WebSMSException(e.getMessage());
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doUpdate(final Context context, final Intent intent) {

		Log.i(TAG, "update");

		this.sendData(context, new ConnectorCommand(intent));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void doSend(final Context context, final Intent intent) {

		Log.i(TAG,
				"send with sender "
						+ Utils.getSender(context,
								new ConnectorCommand(intent).getDefSender()));

		this.sendData(context, new ConnectorCommand(intent));
	}
}
