package com.example.volunhub.org.service;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.volunhub.databinding.FragmentOrgPostServiceBinding;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;
import java.util.TimeZone;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

/**
 * Fragment responsible for allowing organizations to create and post new volunteer services.
 * It handles form validation, date/time selection, and Firestore data entry.
 */
public class OrgPostServiceFragment extends Fragment {

    private static final String TAG = "OrgPostServiceFragment";
    private FragmentOrgPostServiceBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Date selectedServiceDate;

    public OrgPostServiceFragment() {}

    /**
     * Inflates the fragment layout using ViewBinding.
     * @param inflater The LayoutInflater object.
     * @param container The parent view container.
     * @param savedInstanceState Saved state bundle.
     * @return The root view for the fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgPostServiceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Initializes components and sets up UI listeners after the view is created.
     * @param view The created view.
     * @param savedInstanceState Saved state bundle.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupClearErrors();
        setupDatePicker();
        binding.buttonPostService.setOnClickListener(v -> postService());
    }

    /**
     * Configures the click listeners for the date input field and its end icon.
     */
    private void setupDatePicker() {
        binding.editTextPostServiceServiceDate.setOnClickListener(v -> showDatePicker());
        binding.inputLayoutPostServiceServiceDate.setEndIconOnClickListener(v -> showDatePicker());
    }

    /**
     * Displays a Material Design Date Picker and transitions to the Time Picker upon selection.
     */
    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select service date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utcCalendar.setTimeInMillis(selection);
            showTimePicker(utcCalendar);
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    /**
     * Displays a Material Design Time Picker and combines selected time with the chosen date.
     * @param dateCalendar The Calendar object containing the year, month, and day selected previously.
     */
    private void showTimePicker(Calendar dateCalendar) {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select service time")
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();

            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
            calendar.set(Calendar.YEAR, dateCalendar.get(Calendar.YEAR));
            calendar.set(Calendar.MONTH, dateCalendar.get(Calendar.MONTH));
            calendar.set(Calendar.DAY_OF_MONTH, dateCalendar.get(Calendar.DAY_OF_MONTH));
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            selectedServiceDate = calendar.getTime();

            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
            formatter.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
            binding.editTextPostServiceServiceDate.setText(formatter.format(selectedServiceDate));

            binding.inputLayoutPostServiceServiceDate.setError(null);
            binding.inputLayoutPostServiceServiceDate.setErrorEnabled(false);
        });

        timePicker.show(getParentFragmentManager(), "TIME_PICKER");
    }

    /**
     * Validates form inputs and fetches current organization details before saving to Firestore.
     */
    private void postService() {
        String title = getSafeText(binding.editTextPostServiceTitle.getText());
        String description = getSafeText(binding.editTextPostServiceDescription.getText());
        String requirements = getSafeText(binding.editTextPostServiceRequirements.getText());
        String volunteersNeededStr = getSafeText(binding.editTextPostServiceVolunteersNeeded.getText());
        String contactNumber = getSafeText(binding.editTextPostServiceContactNumber.getText());

        if (TextUtils.isEmpty(title)) {
            binding.inputLayoutPostServiceTitle.setError("Title is required");
            return;
        }
        if (TextUtils.isEmpty(description)) {
            binding.inputLayoutPostServiceDescription.setError("Description is required");
            return;
        }
        if (TextUtils.isEmpty(requirements)) {
            binding.inputLayoutPostServiceRequirements.setError("Requirements is required");
            return;
        }
        if (TextUtils.isEmpty(contactNumber)) {
            binding.inputLayoutPostServiceContactNumber.setError("Contact number is required");
            return;
        } else if (contactNumber.length() < 8 || contactNumber.length() > 11) {
            binding.inputLayoutPostServiceContactNumber.setError("Please enter 8 to 11 digits");
            return;
        }

        if (TextUtils.isEmpty(volunteersNeededStr)) {
            binding.inputLayoutPostServiceVolunteersNeeded.setError("Volunteers needed is required");
            return;
        }

        int volunteersNeeded;
        try {
            volunteersNeeded = Integer.parseInt(volunteersNeededStr);
            if (volunteersNeeded <= 0) {
                binding.inputLayoutPostServiceVolunteersNeeded.setError("Must be at least 1");
                return;
            }
        } catch (NumberFormatException e) {
            binding.inputLayoutPostServiceVolunteersNeeded.setError("Invalid number");
            return;
        }

        if (selectedServiceDate == null) {
            binding.inputLayoutPostServiceServiceDate.setError("Service date is required");
            return;
        }

        String orgId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (orgId == null) {
            Toast.makeText(getContext(), "Error: Organization not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(orgId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String orgName = documentSnapshot.getString("orgCompanyName");
                        if (orgName == null) orgName = "Unknown Organization";
                        saveServiceToFirestore(orgId, orgName, title, description, requirements, volunteersNeeded, contactNumber);
                    } else {
                        Toast.makeText(getContext(), "Error: Organization not found.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Constructs the data map and writes a new document to the "services" collection in Firestore.
     * @param orgId The UID of the organization posting the service.
     * @param orgName The name of the organization.
     * @param title Title of the volunteer service.
     * @param description Detailed description of the tasks.
     * @param requirements Necessary skills or items for volunteers.
     * @param volunteersNeeded Total slots available.
     * @param contactNumber Contact phone number for the service.
     */
    private void saveServiceToFirestore(String orgId, String orgName, String title, String description, String requirements, int volunteersNeeded, String contactNumber) {
        Map<String, Object> serviceData = new HashMap<>();
        serviceData.put("orgId", orgId);
        serviceData.put("orgName", orgName);
        serviceData.put("title", title);
        serviceData.put("description", description);
        serviceData.put("requirements", requirements);
        serviceData.put("volunteersNeeded", volunteersNeeded);
        serviceData.put("volunteersApplied", 0);
        serviceData.put("serviceDate", selectedServiceDate);
        serviceData.put("createdAt", FieldValue.serverTimestamp());
        serviceData.put("status", "Active");
        serviceData.put("searchTitle", title.toLowerCase());
        serviceData.put("contactNumber", "+60" + contactNumber);

        db.collection("services")
                .add(serviceData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Service posted successfully!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error posting service: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Error adding document", e);
                });
    }

    /**
     * Cleans up the ViewBinding reference when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Safely retrieves trimmed text from an Editable source, handling null values.
     * @param editable The Editable text from an EditText.
     * @return A trimmed String, or an empty String if null.
     */
    private String getSafeText(android.text.Editable editable) {
        return (editable == null) ? "" : editable.toString().trim();
    }

    /**
     * Attaches text change listeners to all input fields to clear errors dynamically.
     */
    private void setupClearErrors() {
        clearErrorOnType(binding.inputLayoutPostServiceTitle, binding.editTextPostServiceTitle);
        clearErrorOnType(binding.inputLayoutPostServiceDescription, binding.editTextPostServiceDescription);
        clearErrorOnType(binding.inputLayoutPostServiceRequirements, binding.editTextPostServiceRequirements);
        clearErrorOnType(binding.inputLayoutPostServiceVolunteersNeeded, binding.editTextPostServiceVolunteersNeeded);
        clearErrorOnType(binding.inputLayoutPostServiceContactNumber, binding.editTextPostServiceContactNumber);
    }

    /**
     * Helper method to clear the error state of a TextInputLayout as soon as the user types.
     * @param layout The TextInputLayout displaying the error.
     * @param editText The child TextInputEditText being monitored.
     */
    private void clearErrorOnType(com.google.android.material.textfield.TextInputLayout layout,
                                  com.google.android.material.textfield.TextInputEditText editText) {

        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().trim().isEmpty()) {
                    layout.setError(null);
                    layout.setErrorEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
}