package com.tianma.xsmscode.ui.home;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceViewHolder;

import com.github.rove24.xposed.smscode.R;

/**
 * Material 3 Expressive Preference Adapter for Google Messages style grouped cards.
 */
public class M3ExpressivePreferenceAdapter extends PreferenceGroupAdapter {

    public M3ExpressivePreferenceAdapter(PreferenceGroup preferenceGroup) {
        super(preferenceGroup);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        try {
            Preference pref = getItem(position);
            if (pref == null) {
                return;
            }

            View itemView = holder.itemView;

        if (pref instanceof PreferenceCategory) {
            itemView.setBackground(null);
            ViewGroup.LayoutParams rawLp = itemView.getLayoutParams();
            if (rawLp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) rawLp;
                lp.leftMargin = 0;
                lp.rightMargin = 0;
                lp.topMargin = 0;
                lp.bottomMargin = 0;
                itemView.setLayoutParams(lp);
            }
            itemView.setMinimumHeight(0);
            return;
        }

        // Determine position within its category block
        boolean isFirst = (position == 0) || (getItem(position - 1) instanceof PreferenceCategory);
        boolean isLast = (position == getItemCount() - 1) || (getItem(position + 1) instanceof PreferenceCategory);

        int bgRes;
        if (isFirst && isLast) {
            bgRes = R.drawable.bg_m3_card_single;
        } else if (isFirst) {
            bgRes = R.drawable.bg_m3_card_top;
        } else if (isLast) {
            bgRes = R.drawable.bg_m3_card_bottom;
        } else {
            bgRes = R.drawable.bg_m3_card_middle;
        }

        itemView.setBackgroundResource(bgRes);

        // Apply margins
        ViewGroup.LayoutParams rawLp = itemView.getLayoutParams();
        if (rawLp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) rawLp;
            int hMargin = itemView.getResources().getDimensionPixelSize(R.dimen.m3_card_margin_horizontal);
            int bMargin = isLast ? itemView.getResources().getDimensionPixelSize(R.dimen.m3_card_margin_bottom) : 0;
            lp.leftMargin = hMargin;
            lp.rightMargin = hMargin;
            lp.topMargin = 0;
            lp.bottomMargin = bMargin;
            itemView.setLayoutParams(lp);
        }

        float density = itemView.getResources().getDisplayMetrics().density;

        // Compact internal padding matching Google Messages (gentle reduction from previous scheme)
        int padH = itemView.getResources().getDimensionPixelSize(R.dimen.dp_16);
        boolean hasSummary = pref.getSummary() != null && !pref.getSummary().toString().trim().isEmpty();
        int padV = hasSummary ? (int) (2 * density) : (int) (3 * density);
        itemView.setPadding(padH, padV, padH, padV);
        itemView.setMinimumHeight(0);

        // Reset minHeight on all child views inside itemView
        if (itemView instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) itemView;
            for (int i = 0; i < vg.getChildCount(); i++) {
                vg.getChildAt(i).setMinimumHeight(0);
            }
        }

        // Hide empty icon view to align text cleanly to left, like Google Messages
        View iconView = holder.findViewById(android.R.id.icon);
        if (iconView != null && pref.getIcon() == null) {
            iconView.setVisibility(View.GONE);
        }

        // Apply Google Messages typography
        View titleView = holder.findViewById(android.R.id.title);
        if (titleView instanceof android.widget.TextView) {
            android.widget.TextView tv = (android.widget.TextView) titleView;
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f);
            tv.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.m3_on_surface));
            tv.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
            tv.setIncludeFontPadding(false);
        }

        View summaryView = holder.findViewById(android.R.id.summary);
        if (summaryView instanceof android.widget.TextView) {
            android.widget.TextView tv = (android.widget.TextView) summaryView;
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f);
            tv.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.m3_on_surface_variant));
            tv.setIncludeFontPadding(false);
            tv.setLineSpacing(0f, 1f);
            ViewGroup.LayoutParams sLp = tv.getLayoutParams();
            if (sLp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) sLp;
                mlp.topMargin = (int) (1 * density);
                tv.setLayoutParams(mlp);
            }
        }
        } catch (Throwable ignored) {
            // Guard against any runtime styling issues
        }
    }
}