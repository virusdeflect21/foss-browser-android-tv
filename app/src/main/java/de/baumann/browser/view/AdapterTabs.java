package de.baumann.browser.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import de.baumann.browser.R;
import de.baumann.browser.browser.AlbumController;
import de.baumann.browser.browser.BrowserContainer;
import de.baumann.browser.browser.BrowserController;
import de.baumann.browser.unit.HelperUnit;

public class AdapterTabs {

    private final Context context;
    private final AlbumController albumController;
    private View albumView;
    private TextView albumTitle;
    private TextView albumUrl;
    private BrowserController browserController;
    private MaterialCardView albumCardView;

    AdapterTabs(Context context, AlbumController albumController, BrowserController browserController) {
        this.context = context;
        this.albumController = albumController;
        this.browserController = browserController;
        initUI();
    }

    View getAlbumView() {
        return albumView;
    }
    public Object getUrl() {
        return albumUrl.getText().toString();
    }

    void setAlbumTitle(String title, String url) {
        albumTitle.setText(title);
        albumUrl.setText(url);
        HelperUnit.setHighLightedText(context, albumUrl, url, HelperUnit.domain(url));
    }

    void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
    }

    @SuppressLint("InflateParams")
    private void initUI() {
        albumView = LayoutInflater.from(context).inflate(R.layout.item_list, null, false);
        albumCardView = albumView.findViewById(R.id.item_CardViewItem);
        albumTitle = albumView.findViewById(R.id.titleView);
        albumUrl = albumView.findViewById(R.id.dateView);

        // TV: the tab list is a plain container (no ListView selection), so the row itself has
        // to take the D-pad focus and draw the focus ring. Closing a tab from a remote works
        // through the tab's long press menu (D-pad center held down).
        albumView.setFocusable(true);
        // The row root is the RelativeLayout of item_list.xml, so the ViewGroup-only
        // descendant focusability can be set on it.
        ((ViewGroup) albumView).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        albumView.setForeground(ContextCompat.getDrawable(context, R.drawable.tv_focus_ring_rect));

        ImageView albumClose = albumView.findViewById(R.id.iconView);
        albumClose.setImageResource(R.drawable.icon_tab_remove);
        albumClose.setVisibility(View.VISIBLE);
        albumClose.setFocusable(false);
        albumClose.setOnClickListener(view -> {
            browserController.removeAlbum(albumController);
            if (BrowserContainer.size() < 2) {
                browserController.hideOverview();
            }
        });
        assert albumCardView != null;
        albumView.setOnLongClickListener(v -> {
            browserController.showOverflow(null, albumCardView, 1, albumTitle.getText().toString(), albumUrl.getText().toString(), null, null, 0);
            return true;
        });
    }

    public void activate() {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryInverse, typedValue, true);
        int color = typedValue.data;
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
        albumCardView.setCardBackgroundColor(color);
        albumTitle.setTypeface(null, Typeface.BOLD);
        albumView.setOnClickListener(view -> {
            albumCardView.setCardBackgroundColor(color);
            browserController.hideOverview();
        });
    }

    void deactivate() {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHighest, typedValue, true);
        int color = typedValue.data;
        albumCardView.setCardBackgroundColor(color);
        albumTitle.setTypeface(null, Typeface.NORMAL);
        albumView.setOnClickListener(view -> {
            browserController.showAlbum(albumController);
            browserController.hideOverview();
        });
    }
}