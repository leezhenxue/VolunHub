package com.example.volunhub.org.fragments.service;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class OrgPostServiceFragment extends Fragment {

    private static final String TAG = "OrgPostServiceFragment";
    private FragmentOrgPostServiceBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Date selectedServiceDate; // To store the chosen date

    public OrgPostServiceFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgPostServiceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        //added Shao Yee
        setupClearErrors();
        setupDatePicker();
        binding.buttonPostService.setOnClickListener(v -> postService());
    }

    private void setupDatePicker() {
        binding.editTextServiceDate.setOnClickListener(v -> showDatePicker());
        binding.inputLayoutServiceDate.setEndIconOnClickListener(v -> showDatePicker());
    }

    // Replace your old showDatePicker with this updated logic
    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select service date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // 1. Capture the selected date (It comes in UTC)
            Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utcCalendar.setTimeInMillis(selection);

            // 2. Launch the Time Picker immediately
            showTimePicker(utcCalendar);
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    // Add this new method
    private void showTimePicker(Calendar dateCalendar) {
        // Default to 12:00 PM or current time
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select service time")
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();

            // 3. Combine Date + Time
            // We use a "Local" calendar to build the final object
            Calendar finalCalendar = Calendar.getInstance();

            // Copy Year/Month/Day from the Date Picker (UTC)
            finalCalendar.set(Calendar.YEAR, dateCalendar.get(Calendar.YEAR));
            finalCalendar.set(Calendar.MONTH, dateCalendar.get(Calendar.MONTH));
            finalCalendar.set(Calendar.DAY_OF_MONTH, dateCalendar.get(Calendar.DAY_OF_MONTH));

            // Set Hour/Minute from the Time Picker
            finalCalendar.set(Calendar.HOUR_OF_DAY, hour);
            finalCalendar.set(Calendar.MINUTE, minute);
            finalCalendar.set(Calendar.SECOND, 0);
            finalCalendar.set(Calendar.MILLISECOND, 0);

            // 4. Save the final result
            selectedServiceDate = finalCalendar.getTime();

            // 5. Update the UI text
            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
            binding.editTextServiceDate.setText(formatter.format(selectedServiceDate));

            //Shao Yee edited
            // Clear error after selecting a date
            binding.inputLayoutServiceDate.setError(null);
            binding.inputLayoutServiceDate.setErrorEnabled(false);

        });

        timePicker.show(getParentFragmentManager(), "TIME_PICKER");
    }

    private void postService() {
        String title = getSafeText(binding.editTextTitle.getText());
        String description = getSafeText(binding.editTextDescription.getText());
        String requirements = getSafeText(binding.editTextRequirements.getText());
        String volunteersNeededStr = getSafeText(binding.editTextVolunteersNeeded.getText());
        String contactNum = getSafeText(binding.editTextContactNum.getText());

        if (TextUtils.isEmpty(title)) {
            binding.inputLayoutTitle.setError("Title is required");
            return;
        }
        if (TextUtils.isEmpty(description)) {
            binding.inputLayoutDescription.setError("Description is required");
            return;
        }

        // Shao Yee added
        if (TextUtils.isEmpty(requirements)) {
            binding.inputLayoutRequirements.setError("Requirements is required");
            return;
        }
        
        if (TextUtils.isEmpty(volunteersNeededStr)) {
            binding.inputLayoutVolunteersNeeded.setError("Volunteers needed is required");
            return;
        }
        if (TextUtils.isEmpty(contactNum)) {
            binding.inputLayoutContactNum.setError("Contact number is required");
            return;
        }
        if (selectedServiceDate == null) {
            binding.inputLayoutServiceDate.setError("Service date is required");
            return;
        }

        int volunteersNeeded;
        try {
            volunteersNeeded = Integer.parseInt(volunteersNeededStr);
            if (volunteersNeeded <= 0) {
                binding.inputLayoutVolunteersNeeded.setError("Must be at least 1");
                return;
            }
        } catch (NumberFormatException e) {
            binding.inputLayoutVolunteersNeeded.setError("Invalid number");
            return;
        }

        // Get current user's orgId and orgName
        String orgId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (orgId == null) {
            Toast.makeText(getContext(), "Error: Organization not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        // You might need to fetch orgName from your Firestore "users" collection
        // For simplicity, let's assume you have it or fetch it before this
        // Or pass it as an argument if coming from OrgDashboardFragment
        db.collection("users").document(orgId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String orgName = documentSnapshot.getString("orgCompanyName");
                        if (orgName == null) orgName = "Unknown Organization";
                        saveServiceToFireStore(orgId, orgName, title, description, requirements, volunteersNeeded, contactNum);
                    } else {
                        Toast.makeText(getContext(), "Error: Organization not found.", Toast.LENGTH_SHORT).show();
                    }
                });

    }
    public void saveServiceToFireStore(String orgId, String orgName, String title, String description, String requirements, int volunteersNeeded, String contactNum) {
        // Create a new service document in the)
        Map<String, Object> serviceData = new HashMap<>();
        serviceData.put("orgId", orgId);
        serviceData.put("orgName", orgName); // Make sure this is correct
        serviceData.put("title", title);
        serviceData.put("description", description);
        serviceData.put("requirements", requirements);
        serviceData.put("volunteersNeeded", volunteersNeeded);
        serviceData.put("volunteersApplied", 0); // Always start with 0
        serviceData.put("serviceDate", selectedServiceDate);
        serviceData.put("createdAt", FieldValue.serverTimestamp()); // Firestore will set this
        serviceData.put("status", "Active");
        serviceData.put("searchTitle", title.toLowerCase());
        // Qimin: I am saving the contact number so students can reach us
        serviceData.put("contactNum", contactNum);
        Log.d("Qimin_Debug", "Saving contactNum: " + contactNum);

        db.collection("services")
                .add(serviceData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Service posted successfully!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());

                    // Navigate back to the OrgServiceFragment
                    NavController navController = Navigation.findNavController(requireView());
                    navController.popBackStack(); // Go back to previous screen
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error posting service: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Error adding document", e);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Safely gets text from an EditText, trims it, and handles nulls.
     * @param editable The Editable text from binding.editText.getText()
     * @return A trimmed String, or an empty String ("") if it was null.
     */
    private String getSafeText(android.text.Editable editable) {
        return (editable == null) ? "" : editable.toString().trim();
    }


    // SHAO YEE edited

    private void setupClearErrors() {
        clearErrorOnType(binding.inputLayoutTitle, binding.editTextTitle);
        clearErrorOnType(binding.inputLayoutDescription, binding.editTextDescription);
        clearErrorOnType(binding.inputLayoutRequirements, binding.editTextRequirements);
        clearErrorOnType(binding.inputLayoutVolunteersNeeded, binding.editTextVolunteersNeeded);
        clearErrorOnType(binding.inputLayoutContactNum, binding.editTextContactNum);
    }

    private void clearErrorOnType(com.google.android.material.textfield.TextInputLayout layout,
                                  com.google.android.material.textfield.TextInputEditText editText) {

        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Clear error as soon as user types valid text
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
