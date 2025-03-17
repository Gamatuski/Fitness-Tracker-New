package com.example.fitnesstracker.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.fitnesstracker.R;

import java.util.List;


public class CustomSpinnerAdapter extends ArrayAdapter<String> {
    private Context context;
    private List<String> items;
    private int[] icons; // Массив иконок для элементов

    public CustomSpinnerAdapter(Context context, List<String> items, int[] icons) {
        super(context, R.layout.spinner_item, items);
        this.context = context;
        this.items = items;
        this.icons = icons;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.spinner_selected_item, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.spinner_text);
        ImageView arrowIcon = convertView.findViewById(R.id.spinner_arrow);

        textView.setText(getItem(position));
        arrowIcon.setImageResource(R.drawable.ic_arrow_down);

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.spinner_item, parent, false);

        TextView textView = row.findViewById(R.id.item_text);
        ImageView imageView = row.findViewById(R.id.item_icon);

        textView.setText(items.get(position));
        imageView.setImageResource(icons[position]);

        return row;
    }
}
