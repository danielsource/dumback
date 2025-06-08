package core;

import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {
	public static final ResourceBundle messages;

	static {
		Locale defaultLocale = Locale.getDefault();

		if (defaultLocale.getLanguage().equals("pt")) {
			messages = ResourceBundle.getBundle("messages", new Locale("pt", "BR"));
		} else {
			messages = ResourceBundle.getBundle("messages", Locale.ROOT /* English */);
		}
	}

	/* Get localized string */
	public static String i18n(String key) {
		return messages.getString(key);
	}

	public static String i18n(String key, Object... args) {
		return String.format(i18n(key), args);
	}
}
