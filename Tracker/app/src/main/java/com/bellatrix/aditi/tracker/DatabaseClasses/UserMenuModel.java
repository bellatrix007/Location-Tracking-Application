package com.bellatrix.aditi.tracker.DatabaseClasses;

import android.graphics.drawable.Drawable;

public class UserMenuModel {

    public String menuName, displayName;
    public Drawable image;
    public boolean hasChildren, isGroup;

    public UserMenuModel(String menuName, Drawable image, boolean isGroup, boolean hasChildren) {

        this.menuName = menuName;
        this.displayName = menuName;
        this.image = image;
        this.isGroup = isGroup;
        this.hasChildren = hasChildren;
    }

    public UserMenuModel(String menuName, String displayName, Drawable image, boolean isGroup, boolean hasChildren) {

        this.menuName = menuName;
        this.displayName = displayName;
        this.image = image;
        this.isGroup = isGroup;
        this.hasChildren = hasChildren;
    }
}
