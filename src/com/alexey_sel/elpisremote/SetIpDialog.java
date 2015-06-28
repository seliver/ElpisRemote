package com.alexey_sel.elpisremote;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class SetIpDialog extends DialogFragment {
	RemoteControl parent;

	public SetIpDialog(RemoteControl parent) {
		this.parent = parent;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();
		// Inflate and set the layout for the dialog
		// Pass null as the parent view because its going in the dialog layout
		View infView = inflater.inflate(R.layout.setiplayout, null);
		final EditText ipport = (EditText) infView
				.findViewById(R.id.ip);
		ipport.setText(parent.ip);
		builder.setTitle("Set Elpis IP");
		builder.setView(infView)
				// Add action buttons
				.setPositiveButton("Set IP",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								parent.setIpAndConnect(ipport.getText().toString());
							}
						})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								SetIpDialog.this.getDialog().cancel();
							}
						});
		return builder.create();
	}

}
