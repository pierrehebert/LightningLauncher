package net.pierrox.lightning_launcher.prefs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.margaritov.preference.colorpicker.ColorPickerDialog;
import net.margaritov.preference.colorpicker.ColorPickerDialog.OnColorChangedListener;
import net.margaritov.preference.colorpicker.ColorPickerPanelView;
import net.pierrox.lightning_launcher.LLApp;
import net.pierrox.lightning_launcher.LLAppPhone;
import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.script.api.Property;
import net.pierrox.lightning_launcher.engine.variable.Binding;
import net.pierrox.lightning_launcher.views.BoxEditorView;
import net.pierrox.lightning_launcher.views.BoxEditorView.OnBoxEditorEventListener;
import net.pierrox.lightning_launcher_extreme.R;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A list view displaying preferences (instances of {@link LLPreference}).
 */
public class LLPreferenceListView extends ListView implements OnItemClickListener, OnColorChangedListener, DialogInterface.OnClickListener, AdapterView.OnItemLongClickListener, DialogPreferenceSlider.OnDialogPreferenceSliderListener {

	public interface OnLLPreferenceListViewEventListener {
		public void onLLPreferenceClicked(LLPreference preference);
		public void onLLPreferenceLongClicked(LLPreference preference);
		public void onLLPreferenceChanged(LLPreference preference);
		/** @hide */ public void onLLPreferenceBindingRemoved(LLPreferenceBinding preference);
	}
	
	private LLPreferenceColor mDialogColorPreference;
	private LLPreferenceSlider mDialogSliderPreference;
	private LLPreferenceList mDialogListPreference;
	private LLPreferenceText mDialogTextPreference;
	private LLPreference mDialogPreference;

	private Dialog mDialog;
	
	private PrefAdapter mAdapter;

    private boolean mCompactMode;
    private boolean mDisplayOverride;

	private OnLLPreferenceListViewEventListener mOnLLPreferenceListViewEventListener;

	/**
	 * Default constructor.
	 */
	public LLPreferenceListView(Context context, AttributeSet attrs) {
		super(context, attrs);

		setOnItemClickListener(this);
		setOnItemLongClickListener(this);
	}

	/**
	 * @hide
     */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Context context = getContext();
        LLPreference p = (LLPreference)parent.getItemAtPosition(position);
        if(p.isLocked()) {
            LLApp.get().showFeatureLockedDialog(context);
            return;
        }
		if(p.isDisabled()) {
			return;
		}
		if(mDialog != null) {
			mDialog.dismiss();
		}
		mDialog = null;
		if(p instanceof LLPreferenceCheckBox) {
			LLPreferenceCheckBox cbp = (LLPreferenceCheckBox)p;
			cbp.setChecked(!cbp.isChecked());
			mAdapter.notifyDataSetChanged();
			if(mOnLLPreferenceListViewEventListener != null) mOnLLPreferenceListViewEventListener.onLLPreferenceChanged(cbp);
		} else if(p instanceof LLPreferenceColor) {
			mDialogColorPreference = (LLPreferenceColor)p;
			ColorPickerDialog color_picker_dialog = new ColorPickerDialog(context, mDialogColorPreference.getColor());
			color_picker_dialog.setAlphaSliderVisible(mDialogColorPreference.hasAlpha());
			color_picker_dialog.setOnColorChangedListener(this);
			mDialog = color_picker_dialog;
		} else if(p instanceof LLPreferenceSlider) {
			mDialogSliderPreference = (LLPreferenceSlider)p;
			LLPreferenceSlider sp = mDialogSliderPreference;
			DialogPreferenceSlider slider_dialog = new DialogPreferenceSlider(context, sp.getValue(), sp.getValueType()== LLPreferenceSlider.ValueType.FLOAT, sp.getMinValue(), sp.getMaxValue(), sp.getInterval(), sp.getUnit(), this);
			slider_dialog.setTitle(sp.getTitle());
			mDialog = slider_dialog;
        } else if(p instanceof LLPreferenceText) {
            mDialogTextPreference = (LLPreferenceText)p;
            LLPreferenceText tp = mDialogTextPreference;
            LLPreferenceTextDialog text_dialog = new LLPreferenceTextDialog(context, tp.getValue());
            text_dialog.setTitle(tp.getTitle());
            mDialog = text_dialog;
		} else if(p instanceof LLPreferenceList) {
			mDialogListPreference = (LLPreferenceList)p;
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(mDialogListPreference.getTitle());
			builder.setSingleChoiceItems(mDialogListPreference.getLabels(), mDialogListPreference.getValueIndex(), this);
			builder.setNegativeButton(android.R.string.cancel, null);
			mDialog = builder.create();
        }
		
		if(mDialog != null) {
            mDialogPreference = p;
			mDialog.show();
		}
		
		if(mOnLLPreferenceListViewEventListener != null) mOnLLPreferenceListViewEventListener.onLLPreferenceClicked(p);
	}

	/**
	 * @hide
	 */
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        LLPreference p = (LLPreference)parent.getItemAtPosition(position);
		if(mOnLLPreferenceListViewEventListener != null) mOnLLPreferenceListViewEventListener.onLLPreferenceLongClicked(p);
		return true;
	}

	/**
	 * @hide
	 */
	@Override
	public void onColorChanged(int color) {
		mDialogColorPreference.setColor(color);
		mAdapter.notifyDataSetChanged();
		if(mOnLLPreferenceListViewEventListener != null) mOnLLPreferenceListViewEventListener.onLLPreferenceChanged(mDialogColorPreference);
	}

	/**
	 * @hide
	 */
    @Override
    public void onColorDialogSelected(int color) {
        // pass
    }

	/**
	 * @hide
	 */
    @Override
    public void onColorDialogCanceled() {
        // pass
    }

	/**
	 * @hide
	 */
    @Override
	public void onClick(DialogInterface dialog, int which) {
		mDialogListPreference.setValueIndex(which);
		dialog.dismiss();
		mAdapter.notifyDataSetChanged();
		if(mOnLLPreferenceListViewEventListener != null) mOnLLPreferenceListViewEventListener.onLLPreferenceChanged(mDialogListPreference);
	}

	/**
	 * Set a listener for preference clicked, long clicked, changed events
     */
	public void setListener(OnLLPreferenceListViewEventListener listener) {
		mOnLLPreferenceListViewEventListener = listener;
	}

	/**
	 * Set the list of preferences to display in this listview.
     */
	public void setPreferences(LLPreference[] preferences) {
		setPreferences(new ArrayList<>(Arrays.asList(preferences)));
	}

	/**
	 * @hide
	 */
	public void setPreferences(ArrayList<LLPreference> preferences) {
		mAdapter = new PrefAdapter(getContext(), 0, preferences);
		setAdapter(mAdapter);
	}

	/**
	 * @hide
	 */
    public ArrayList<LLPreference> getPreferences() {
        return mAdapter == null ? null : mAdapter.getObjects();
    }

	/**
	 * Request a manual refresh.
	 */
	public void refresh() {
		int count = mAdapter.getCount();
		mAdapter.notifyDataSetChanged();
		if(mAdapter.getCount() != count) {
			// this is needed to force the ListView to keep its children views in sync by clearing its recycler
			setAdapter(mAdapter);
		}
	}

	/**
	 * @hide
	 */
    @Override
    public void onDialogPreferenceSliderValueSet(float value) {
        mDialogSliderPreference.setValue(value);
        mAdapter.notifyDataSetChanged();
        if(mOnLLPreferenceListViewEventListener != null) mOnLLPreferenceListViewEventListener.onLLPreferenceChanged(mDialogSliderPreference);
    }

	/**
	 * @hide
	 */
    @Override
    public void onDialogPreferenceSliderCancel() {
        // pass
    }

	/**
	 * Use a mode where preferences are displayed using a more compact layout
	 * @param compactMode true to shrink preferences and display more preferences in a screen.
     */
    public void setCompactMode(boolean compactMode) {
        mCompactMode = compactMode;
    }

	/**
	 * Select whether to display the override checkbow at the left of the preference. This implies that preferences have a default value so that this override checkbox can be managed properly.
	 */
    public void setDisplayOverride(boolean displayOverride) {
        mDisplayOverride = displayOverride;
        if(mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

	private class PrefAdapter extends ArrayAdapter<LLPreference> implements OnCheckedChangeListener, OnBoxEditorEventListener, OnClickListener {
		private int mPrefLayout;
		private int mPrefCategoryLayout;
		private int mPrefListWidgetLayout;
        private float mPrefSizeCategory;
        private float mPrefSizeTitle;
        private float mPrefSizeSummary;
		private LayoutInflater mLayoutInflater;
		private float mDensity;
        ArrayList<LLPreference> mObjects;
        ArrayList<LLPreference> mFilteredObjects;

		public PrefAdapter(Context context, int textViewResourceId, ArrayList<LLPreference> objects) {
			super(context, textViewResourceId);
			mPrefLayout = new Preference(context).getLayoutResource();
			mPrefCategoryLayout = new PreferenceCategory(context).getLayoutResource();
			mPrefListWidgetLayout = new ListPreference(context).getWidgetLayoutResource();
			mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            Resources resources = getResources();
            DisplayMetrics displayMetrics = resources.getDisplayMetrics();
            mDensity = displayMetrics.density;
            float scaledDensity = displayMetrics.scaledDensity;
            mPrefSizeCategory = resources.getDimensionPixelSize(R.dimen.pref_size_category) / scaledDensity;
            mPrefSizeTitle = resources.getDimensionPixelSize(R.dimen.pref_size_title) / scaledDensity;
            mPrefSizeSummary = resources.getDimensionPixelSize(R.dimen.pref_size_summary) / scaledDensity;
            mObjects = objects;
            filterObjects();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LLPreference p = getItem(position);
			boolean is_category = p instanceof LLPreferenceCategory;
			
			if(p instanceof LLPreferenceBox) {
				if(convertView == null) {
					convertView = mLayoutInflater.inflate(R.layout.box_config, null);
					((TextView)convertView.findViewById(R.id.box_h)).setText(R.string.b_hint);
					((BoxEditorView)convertView.findViewById(R.id.box)).setOnBoxEditorEventListener(p, this);
				}
			} else {
				if(convertView == null) {
					View preference_view = mLayoutInflater.inflate(is_category ? mPrefCategoryLayout : mPrefLayout, null);
                    if(mCompactMode) {
                        TextView title_view = (TextView) preference_view.findViewById(android.R.id.title);
                        if (is_category) {
                            title_view.setTextSize(mPrefSizeCategory);
                        } else {
                            ((View) title_view.getParent()).setPadding(0, 0, 0, 0);
                            title_view.setTextSize(mPrefSizeTitle);
                            ((TextView) preference_view.findViewById(android.R.id.summary)).setTextSize(mPrefSizeSummary);
                            preference_view.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.pref_height));
                        }
                    }
                    preference_view = ((LLAppPhone)LLApp.get()).managePreferenceViewLockedFlag(p, preference_view);
					View icon = preference_view.findViewById(android.R.id.icon);
					if(icon != null) {
                        ((View)icon.getParent()).setVisibility(View.GONE);
					}
					
					ViewGroup widget_frame = (ViewGroup) preference_view.findViewById(android.R.id.widget_frame);
					
					if(is_category) {
						//preference_view.setEnabled(false);
					} else if(p instanceof LLPreferenceList) {
						if(mPrefListWidgetLayout != 0) {
							mLayoutInflater.inflate(mPrefListWidgetLayout, widget_frame);
						}
					} else if(p instanceof LLPreferenceCheckBox) {
						CheckBox widget = new CheckBox(getContext());
						widget.setFocusable(false);
						widget.setClickable(false);
						widget_frame.addView(widget);
					} else if(p instanceof LLPreferenceColor) {
						View v = new ColorPickerPanelView(getContext());
						int s = (int)(30*mDensity);
						v.setLayoutParams(new FrameLayout.LayoutParams(s, s));
						widget_frame.addView(v);
					} else if(p instanceof LLPreferenceSlider) {
                        mLayoutInflater.inflate(R.layout.llpref_slider, widget_frame);
                    } else if(p instanceof LLPreferenceBinding) {
                        Button widget = new Button(getContext());
                        widget.setText("4");
                        widget.setTypeface(LLApp.get().getIconsTypeface());
                        widget.setOnClickListener(this);
                        widget.setFocusable(false);
                        widget_frame.addView(widget);
					}
					
				
					convertView = mLayoutInflater.inflate(R.layout.override_preference, null, false);
					CheckBox override = (CheckBox)convertView.findViewById(R.id.override);
					override.setOnCheckedChangeListener(this);
					LinearLayout container = (LinearLayout)convertView.findViewById(R.id.content);
					container.addView(preference_view, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
				}
				
				View override_g = convertView.findViewById(R.id.override_g);
                TextView override_t = (TextView) override_g.findViewById(R.id.override_t);
				CheckBox override = (CheckBox)convertView.findViewById(R.id.override);
				override.setTag(p);
				if(!is_category && p.isShowingOverride() && mDisplayOverride) {
					boolean overriding = p.isOverriding();
					boolean enabled = overriding && !p.isDisabled();
					override.setChecked(overriding);
					override.setEnabled(enabled);
                    override_t.setEnabled(enabled);
                    override_t.setText(R.string.ovr_custom);
					override_g.setVisibility(View.VISIBLE);
				} else {
					override_g.setVisibility(is_category || !mDisplayOverride ? View.GONE : View.INVISIBLE);
				}

                String title;
                if(p instanceof LLPreferenceBinding) {
                    Binding b = ((LLPreferenceBinding)p).getValue();
                    Property prop = Property.getByName(b.target);
                    title = prop == null ? p.getTitle() : prop.getLabel();
                    override.setChecked(b.enabled);
                    override.setEnabled(true);
                    override_t.setEnabled(true);
                    override_t.setText(R.string.pe);
                    override_g.setVisibility(View.VISIBLE);
                } else {
                    title = p.getTitle();
                }
                ((TextView) convertView.findViewById(android.R.id.title)).setText(title);
				if(!is_category) {
					String s = p.getSummary();
					TextView vs = (TextView) convertView.findViewById(android.R.id.summary);
					if(s == null) {
						vs.setVisibility(View.GONE);
					} else {
						vs.setVisibility(View.VISIBLE);
						vs.setText(s);
					}
					
				}
				
				ViewGroup widget_frame = (ViewGroup) convertView.findViewById(android.R.id.widget_frame);
				if(is_category) {
				} else if(p instanceof LLPreferenceCheckBox) {
					CheckBox widget = (CheckBox) widget_frame.getChildAt(0);
					widget.setChecked(((LLPreferenceCheckBox)p).isChecked());
				} else if(p instanceof LLPreferenceColor) {
//					View color_preview = widget_frame.findViewById(R.id.color_preview);
//					color_preview.setBackgroundColor(((LLPreferenceColor)p).getColor());
					ColorPickerPanelView v = (ColorPickerPanelView)widget_frame.getChildAt(0);
					v.setColor(((LLPreferenceColor)p).getColor());
				} else if(p instanceof LLPreferenceSlider) {
                    TextView tv_value = (TextView) widget_frame.findViewById(R.id.slider_value);
                    TextView tv_unit = (TextView) widget_frame.findViewById(R.id.slider_unit);
                    LLPreferenceSlider sp = (LLPreferenceSlider) p;
                    String unit = sp.getUnit();
                    tv_value.setText(DialogPreferenceSlider.valueAsText(sp.getValueType()== LLPreferenceSlider.ValueType.FLOAT, unit, sp.getValue(), sp.getInterval()));
                    tv_unit.setText(sp.getUnit());
                } else if(p instanceof LLPreferenceBinding) {
                    widget_frame.getChildAt(0).setTag(p);
				}
			}
			
			if(!is_category) {
				// should use dimensions from some resource
				convertView.setPadding(15, 0, 10, 0);
			}

			setEnabledStateOnViews(convertView, !p.isDisabled());
			
			return convertView;
		}
		
		private void setEnabledStateOnViews(View v, boolean enabled) {
		    v.setEnabled(enabled);

		    if (v instanceof ViewGroup) {
		        final ViewGroup vg = (ViewGroup) v;
		        for (int i = vg.getChildCount() - 1; i >= 0; i--) {
		        	View c = vg.getChildAt(i);
		        	// do not change the override checkbox
		        	int id=c.getId();
		        	if(id!=R.id.override && id!=R.id.override_t) {
		        		setEnabledStateOnViews(c, enabled);
		        	}
		        }
		    }
		}

        @Override
        public void notifyDataSetChanged() {
            filterObjects();
            super.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mFilteredObjects.size();
        }

        @Override
        public LLPreference getItem(int position) {
            return mFilteredObjects.get(position);
        }

        @Override
		public boolean isEnabled(int position) {
			return !(getItem(position) instanceof LLPreferenceCategory);
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public int getItemViewType(int position) {
			LLPreference p = getItem(position);
			if(p instanceof LLPreferenceCategory) {
				return 1;
			} else if(p instanceof LLPreferenceCheckBox) {
				return 2;
			} else if(p instanceof LLPreferenceColor) {
				return 3;
			} else if(p instanceof LLPreferenceList) {
				return 4;
			} else if(p instanceof LLPreferenceSlider) {
				return 5;
			} else if(p instanceof LLPreferenceBox) {
				return 6;
			} else if(p instanceof LLPreferenceBinding) {
				return 7;
			}
			return 0;
		}

		@Override
		public int getViewTypeCount() {
			// base pref, category, checkbox, color, slider, list, box, binding
			return 8;
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            LLPreference p = (LLPreference)buttonView.getTag();
            if(p instanceof LLPreferenceBinding) {
                Binding binding = ((LLPreferenceBinding) p).getValue();
                if(binding.enabled != isChecked) {
                    binding.enabled = isChecked;
                    if (mOnLLPreferenceListViewEventListener != null) mOnLLPreferenceListViewEventListener.onLLPreferenceChanged(p);
                }
            } else {
                if (!isChecked) {
                    if (p.isOverriding()) {
                        p.reset();
                        notifyDataSetChanged();
                        if (mOnLLPreferenceListViewEventListener != null) mOnLLPreferenceListViewEventListener.onLLPreferenceChanged(p);
                    }
                }
            }
		}

		@Override
		public void onBoxSelectionChanged(Object token, int selection) {
			LLPreferenceBox p = (LLPreferenceBox)token;
			p.setSelection(selection);
			if(mOnLLPreferenceListViewEventListener != null) mOnLLPreferenceListViewEventListener.onLLPreferenceChanged(p);
		}

        private void filterObjects() {
            mFilteredObjects = new ArrayList<>();
            for(LLPreference p : mObjects) {
                if(p.isVisible()) {
                    mFilteredObjects.add(p);
                }
            }
        }

        @Override
        public void onClick(View v) {
            LLPreferenceBinding p = (LLPreferenceBinding) v.getTag();
            if(mOnLLPreferenceListViewEventListener != null) mOnLLPreferenceListViewEventListener.onLLPreferenceBindingRemoved(p);
        }

        public ArrayList<LLPreference> getObjects() {
            return mObjects;
        }
    }

    private class LLPreferenceTextDialog extends AlertDialog implements DialogInterface.OnClickListener {
        private String mValue;
        private EditText mDialogEditText;

        public LLPreferenceTextDialog(Context context, String value) {
            super(context);

            mValue = value;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            Context context = getContext();

            mDialogEditText = new EditText(context);
            FrameLayout l = new FrameLayout(context);
            l.setPadding(10, 10, 10, 10);
            l.addView(mDialogEditText);
            mDialogEditText.setText(mValue);
            mDialogEditText.setSelection(mValue.length());
            setView(l);

            setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok), this);
            setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), this);

            super.onCreate(savedInstanceState);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch(which) {
                case BUTTON_POSITIVE:
                    mDialogTextPreference.setValue(mDialogEditText.getText().toString());
                    mAdapter.notifyDataSetChanged();
                    if(mOnLLPreferenceListViewEventListener != null) mOnLLPreferenceListViewEventListener.onLLPreferenceChanged(mDialogTextPreference);
                    break;
            }
        }
    }
}
