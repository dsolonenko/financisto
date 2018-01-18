/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.view;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.model.Attribute;
import ru.orangesoftware.financisto.model.TransactionAttribute;
import ru.orangesoftware.financisto.view.NodeInflater.Builder;
import ru.orangesoftware.financisto.view.NodeInflater.CheckBoxBuilder;

public abstract class AttributeView implements OnClickListener {
    public final Attribute attribute;

    protected final Context context;
    protected final NodeInflater inflater;

    public AttributeView(Context context, Attribute attribute) {
        this.context = context;
        this.attribute = attribute;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.inflater = new NodeInflater(layoutInflater);
    }

    @Override
    public void onClick(View view) {
        // do nothing by default
    }

    public abstract View inflateView(LinearLayout layout, String value);

    public abstract String value();

    public TransactionAttribute newTransactionAttribute() {
        TransactionAttribute ta = new TransactionAttribute();
        ta.attributeId = attribute.id;
        ta.value = value();
        return ta;
    }

}

class TextAttributeView extends AttributeView {

    private EditText editText;

    public TextAttributeView(Context context, Attribute attribute) {
        super(context, attribute);
    }

    @Override
    public View inflateView(LinearLayout layout, String value) {
        editText = new EditText(context);
        editText.setSingleLine();
        if (value != null) {
            editText.setText(value);
        }
        return inflater.new EditBuilder(layout, editText).withLabel(attribute.title).create();
    }

    @Override
    public String value() {
        return editText.getText().toString();
    }

}

class NumberAttributeView extends AttributeView {

    private EditText editText;

    public NumberAttributeView(Context context, Attribute attribute) {
        super(context, attribute);
    }

    @Override
    public View inflateView(LinearLayout layout, String value) {
        editText = new EditText(context);
        editText.setSingleLine();
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (value != null) {
            editText.setText(value);
        }
        return inflater.new EditBuilder(layout, editText).withLabel(attribute.title).create();
    }

    @Override
    public String value() {
        return editText.getText().toString();
    }

}

class ListAttributeView extends AttributeView {

    private final String[] items;
    private int selectedIndex = -1;

    public ListAttributeView(Context context, Attribute attribute) {
        super(context, attribute);
        items = attribute.listValues != null ? attribute.listValues.split(";") : new String[0];
    }

    @Override
    public View inflateView(LinearLayout layout, String value) {
        Builder b = inflater.new ListBuilder(layout, R.layout.select_entry);
        b.withId(R.id.click_attribute, this);
        if (value != null) {
            b.withData(value);
            for (int i = 0; i < items.length; i++) {
                if (items[i].equalsIgnoreCase(value)) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        return b.withLabel(attribute.title).create();
    }

    @Override
    public void onClick(final View view) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.select_dialog_singlechoice, items);
        new AlertDialog.Builder(context)
                .setSingleChoiceItems(adapter, selectedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    selectedIndex = which;
                    TextView text = view.findViewById(R.id.data);
                    text.setText(items[which]);
                })
                .setTitle(attribute.title)
                .show();

    }

    @Override
    public String value() {
        return selectedIndex == -1 ? null : items[selectedIndex];
    }

}

class CheckBoxAttributeView extends AttributeView {

    private boolean checked = false;

    public CheckBoxAttributeView(Context context, Attribute attribute) {
        super(context, attribute);
    }

    @Override
    public View inflateView(LinearLayout layout, String value) {
        CheckBoxBuilder b = inflater.new CheckBoxBuilder(layout);
        checked = Boolean.valueOf(value);
        b.withCheckbox(checked);
        b.withId(R.id.click_attribute, this);
        b.withLabel(attribute.title);
        if (attribute.listValues != null) {
            b.withData(attribute.listValues.replace(';', '/'));
        } else {
            b.withData(R.string.checkbox_values);
        }
        return b.withLabel(attribute.title).create();
    }

    @Override
    public void onClick(View view) {
        checked = !checked;
        CheckBox b = view.findViewById(R.id.checkbox);
        b.setChecked(checked);
    }

    @Override
    public String value() {
        return checked ? Boolean.TRUE.toString() : Boolean.FALSE.toString();
    }

}
