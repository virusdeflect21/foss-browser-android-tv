package de.baumann.browser.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import de.baumann.browser.R;
import de.baumann.browser.database.FaviconHelper;
import de.baumann.browser.database.Record;
import de.baumann.browser.unit.HelperUnit;

public class AdapterSearch extends BaseAdapter implements Filterable {
    private final Context context;
    private final int layoutResId;
    private final List<CompleteItem> originalList = new ArrayList<>();
    private final CompleteFilter filter = new CompleteFilter();
    private final SharedPreferences sp;
    private final FaviconHelper faviconHelper;
    private final int cardBackgroundColor;
    private List<CompleteItem> resultList = new ArrayList<>();

    public AdapterSearch(Context context, int layoutResId, List<Record> recordList) {
        this.context = context;
        this.layoutResId = layoutResId;
        this.sp = PreferenceManager.getDefaultSharedPreferences(context);
        this.faviconHelper = new FaviconHelper(context);

        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHighest, typedValue, true);
        this.cardBackgroundColor = typedValue.data;

        getRecordList(recordList);
    }

    private void getRecordList(List<Record> recordList) {
        if (recordList == null) return;

        HashSet<CompleteItem> uniqueItems = new HashSet<>();
        for (Record record : recordList) {
            String title = record.getTitle();
            String url = record.getURL();
            if (title != null && !title.isEmpty() && url != null && !url.isEmpty()) {
                uniqueItems.add(new CompleteItem(title, url));
            }
        }
        originalList.addAll(uniqueItems);
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public Object getItem(int position) {
        return resultList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        Holder holder;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
            holder = new Holder();
            holder.titleView = view.findViewById(R.id.titleView);
            holder.urlView = view.findViewById(R.id.dateView);
            holder.favicon = view.findViewById(R.id.item_icon);
            holder.albumCardView = view.findViewById(R.id.item_CardViewItem);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        CompleteItem item = resultList.get(position);
        holder.titleView.setText(item.title);
        holder.urlView.setText(item.url);
        holder.albumCardView.setCardBackgroundColor(cardBackgroundColor);

        try {
            Bitmap bitmap = faviconHelper.getFavicon(item.url);
            if (bitmap != null && !bitmap.isRecycled()) {
                holder.favicon.setImageBitmap(bitmap);
            } else {
                holder.favicon.setImageResource(R.drawable.icon_image_broken);
            }
        } catch (Exception e) {
            holder.favicon.setImageResource(R.drawable.icon_image_broken);
        }

        String s = sp.getString("searchInput", "");
        if (!s.isEmpty()) {
            HelperUnit.setHighLightedTextSearch(context, holder.urlView, s);
            HelperUnit.setHighLightedTextSearch(context, holder.titleView, s);
        }

        return view;
    }

    private static class CompleteItem {
        private final String title;
        private final String url;
        private final String lowerTitle;
        private final String lowerUrl;
        private int index = Integer.MAX_VALUE;

        private CompleteItem(String title, String url) {
            this.title = title;
            this.url = url;
            this.lowerTitle = title.toLowerCase(Locale.ROOT);
            this.lowerUrl = url.toLowerCase(Locale.ROOT);
        }

        int getIndex() { return index; }
        void setIndex(int index) { this.index = index; }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof CompleteItem item)) return false;
            return item.title.equals(title) && item.url.equals(url);
        }

        @Override
        public int hashCode() {
            return title.hashCode() ^ url.hashCode();
        }
    }

    private static class Holder {
        private ImageView favicon;
        private TextView titleView;
        private TextView urlView;
        private MaterialCardView albumCardView;
    }

    private class CompleteFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();
            if (prefix == null || prefix.length() == 0) {
                results.values = new ArrayList<>();
                results.count = 0;
                return results;
            }

            String searchStr = prefix.toString().toLowerCase(Locale.ROOT);
            List<CompleteItem> workList = new ArrayList<>();

            for (CompleteItem item : originalList) {
                boolean matchesTitle = item.lowerTitle.contains(searchStr);
                boolean matchesUrl = item.lowerUrl.contains(searchStr);

                if (matchesTitle || matchesUrl) {
                    if (matchesTitle) {
                        item.setIndex(item.lowerTitle.indexOf(searchStr));
                    } else {
                        item.setIndex(item.lowerUrl.indexOf(searchStr));
                    }
                    workList.add(item);
                }
            }
            workList.sort(Comparator.comparingInt(CompleteItem::getIndex).reversed());

            results.values = workList;
            results.count = workList.size();
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results != null && results.count > 0) {
                resultList = (List<CompleteItem>) results.values;
                notifyDataSetChanged();
            } else {
                resultList = new ArrayList<>();
                notifyDataSetInvalidated();
            }
        }
    }
}