package de.baumann.browser.view;

public class MenuItem {
    private final String title;
    private int iconResId;
    private boolean isSelected;

    public MenuItem(String title, int iconResId, boolean isSelected) {
        this.title = title;
        this.iconResId = iconResId;
        this.isSelected = isSelected;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public String getTitle() { return title; }
    public int getIconResId() { return iconResId; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}