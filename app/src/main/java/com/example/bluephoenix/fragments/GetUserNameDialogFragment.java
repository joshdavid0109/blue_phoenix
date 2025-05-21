package com.example.bluephoenix.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.bluephoenix.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class GetUserNameDialogFragment extends DialogFragment {

    private NameInputDialogListener listener;

    public interface NameInputDialogListener {
        void onNameEntered(String name);
    }

    public static GetUserNameDialogFragment newInstance(String title) {
        GetUserNameDialogFragment fragment = new GetUserNameDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        fragment.setArguments(args);
        return fragment;
    }

    public GetUserNameDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NameInputDialogListener) {
            listener = (NameInputDialogListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement NameInputDialogListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false); // Prevent dismissing by tapping outside or pressing back
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_get_user_name, null);

        TextInputEditText editTextName = view.findViewById(R.id.editTextName);
        MaterialButton buttonSubmit = view.findViewById(R.id.buttonSubmit);

        builder.setView(view);

        Dialog dialog = builder.create();

        buttonSubmit.setOnClickListener(v -> {
            String name = editTextName.getText().toString().trim();
            if (listener != null && !name.isEmpty()) {
                listener.onNameEntered(name);
                dialog.dismiss(); // Dismiss the dialog after submission
            } else {
                // Optionally, show an error message if the name is empty
                editTextName.setError("Please enter your name");
            }
        });

        return dialog;
    }
}