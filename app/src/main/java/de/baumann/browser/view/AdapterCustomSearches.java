package de.baumann.browser.view;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

import java.util.ArrayList;

import de.baumann.browser.R;
import de.baumann.browser.activity.BrowserActivity;
import de.baumann.browser.objects.CustomRedirect;
import de.baumann.browser.objects.CustomSearchesHelper;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;

public class AdapterCustomSearches extends RecyclerView.Adapter<RedirectsViewHolder> {
    final private ArrayList<CustomRedirect> redirects;
    private final Context context;
    private final String url;

    public AdapterCustomSearches(Context context, String url, ArrayList<CustomRedirect> redirects) {
        super();
        this.redirects = redirects;
        this.context = context;
        this.url = url;
    }

    @NonNull
    @Override
    public RedirectsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list, parent, false);
        return new RedirectsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RedirectsViewHolder holder, int position) {
        CustomRedirect current = redirects.get(position);
        TextView source = holder.itemView.findViewById(R.id.titleView);
        TextView target = holder.itemView.findViewById(R.id.dateView);
        ImageView remove = holder.itemView.findViewById(R.id.iconView);

        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHighest, typedValue, true);
        int color = typedValue.data;
        CardView albumCardView = holder.itemView.findViewById(R.id.item_CardViewItem);
        albumCardView.setCardBackgroundColor(color);
        CardView cardView = holder.itemView.findViewById(R.id.item_cardView);
        cardView.setVisibility(GONE);



        remove.setVisibility(VISIBLE);
        source.setText(current.getSource());
        target.setText(current.getTarget());
        HelperUnit.setHighLightedText(context, target, current.getTarget(), HelperUnit.domain(current.getTarget()));

        remove.setOnClickListener(v -> {
            Snackbar snackbar = Snackbar.make(holder.itemView, R.string.hint_database, Snackbar.LENGTH_SHORT);
            HelperUnit.makeSnackbarRound(snackbar);
            snackbar.setAction(context.getString(R.string.app_ok), (v2 -> {
                redirects.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, getItemCount());
                try {
                    CustomSearchesHelper.saveRedirects(redirects);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }));
            snackbar.show();
        });
        holder.itemView.setOnClickListener(v -> {
            NinjaWebView.getBrowserController().hideSearch();
            // 1. Hole die Custom-Such-URL (z.B. "https://duckduckgo.com")
            String targetUrl = current.getTarget();
            if (targetUrl == null || targetUrl.isEmpty()) return;
            // 2. Den eingegebenen Text für URLs sicher codieren (Satzzeichen, Leerzeichen, Absätze)
            String encodedQuery;
            try {
                encodedQuery = java.net.URLEncoder.encode(url, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                Log.e("CustomSearch", "Encoding failed", e);
                encodedQuery = url; // Fallback
            }
            // 3. Die finale URL zusammenbauen
            String finalUrl;
            if (targetUrl.contains("%s")) {
                // Falls deine Custom-URL ein %s als Platzhalter nutzt
                finalUrl = targetUrl.replace("%s", encodedQuery);
            } else {
                // Falls der Suchbegriff einfach hinten angehängt werden soll
                finalUrl = targetUrl + encodedQuery;
            }
            // 4. Die URL im Browser öffnen
            if (BrowserUnit.isURL(finalUrl)) {
                BrowserUnit.intentURL(context, Uri.parse(finalUrl));
            } else {
                NinjaToast.show(BrowserActivity.getAppContext(), R.string.app_error);
            }
        });
    }

    @Override
    public int getItemCount() {
        return redirects.size();
    }

    public ArrayList<CustomRedirect> getRedirects() {
        return redirects;
    }

    public void addRedirect(CustomRedirect redirect) {
        redirects.add(redirect);
        notifyItemInserted(getItemCount() - 1);
    }
}

