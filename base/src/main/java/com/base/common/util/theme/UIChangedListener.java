package com.base.common.util.theme;

public interface UIChangedListener {

    default void onThemeChanged(Boolean withAnim) {}

    default void onLanguageChanged(Boolean withAnim) {}

}
