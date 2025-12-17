package com.example.volunhub.org;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.volunhub.databinding.FragmentOrgPostServiceBinding;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
// Qimin: Using update instead of set to preserve other fields (like applicant counts) that we don't want to overwrite.
public class EditJobActivity extends AppCompatActivity {

    private static final String TAG = "EditJobActivity";
    private FragmentOrgPostServiceBinding binding;
    private FirebaseFirestore db;
    private String serviceId;
    private Date selectedServiceDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Reuse the existing layout
        binding = FragmentOrgPostServiceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        // Get service details from Intent
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Toast.makeText(this, "Error: No service data provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        serviceId = extras.getString("serviceId");
        if (serviceId == null || serviceId.isEmpty()) {
            Toast.makeText(this, "Error: Service ID is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Pre-fill the form with existing data
        prefillForm(extras);

        // Change button text from "Post" to "Save Changes"
        binding.buttonPostService.setText("Save Changes");

        // Setup date picker
        setupDatePicker();

        // Setup button click listener
        binding.buttonPostService.setOnClickListener(v -> saveChanges());
    }

    /**
     * Pre-fills the form fields with existing service data from Intent extras.
     */
    private void prefillForm(Bundle extras) {
        // Pre-fill title
        String title = extras.getString("title");
        if (title != null) {
            binding.editTextPostServiceTitle.setText(title);
        }

        // Pre-fill description
        String desc = extras.getString("desc");
        if (desc == null) {
            desc = extras.getString("description"); // Try alternative key
        }
        if (desc != null) {
            binding.editTextPostServiceDescription.setText(desc);
        }

        // Pre-fill requirements
        String requirements = extras.getString("requirements");
        if (requirements != null) {
            binding.editTextPostServiceRequirements.setText(requirements);
        }

        // Pre-fill volunteers needed
        String volunteersNeeded = extras.getString("volunteersNeeded");
        if (volunteersNeeded == null) {
            // Try as integer
            int volunteers = extras.getInt("volunteersNeeded", -1);
            if (volunteers > 0) {
                volunteersNeeded = String.valueOf(volunteers);
            }
        }
        if (volunteersNeeded != null) {
            binding.editTextPostServiceVolunteersNeeded.setText(volunteersNeeded);
        }

        // Pre-fill service date
        long serviceDateMillis = extras.getLong("serviceDate", -1);
        if (serviceDateMillis > 0) {
            selectedServiceDate = new Date(serviceDateMillis);
            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
            binding.editTextPostServiceServiceDate.setText(formatter.format(selectedServiceDate));
        }

        // Qimin: I am pre-filling the contact number so I don't lose it
        String contact = extras.getString("contact");
        if (contact != null) {
            binding.editTextPostServiceContactNumber.setText(contact);
            Log.d("Qimin_Debug", "Loaded contact for editing: " + contact);
        }
    }

    private void setupDatePicker() {
        binding.editTextPostServiceServiceDate.setOnClickListener(v -> showDatePicker());
        binding.inputLayoutPostServiceServiceDate.setEndIconOnClickListener(v -> showDatePicker());
    }

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

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

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

            Calendar finalCalendar = Calendar.getInstance();
            finalCalendar.set(Calendar.YEAR, dateCalendar.get(Calendar.YEAR));
            finalCalendar.set(Calendar.MONTH, dateCalendar.get(Calendar.MONTH));
            finalCalendar.set(Calendar.DAY_OF_MONTH, dateCalendar.get(Calendar.DAY_OF_MONTH));
            finalCalendar.set(Calendar.HOUR_OF_DAY, hour);
            finalCalendar.set(Calendar.MINUTE, minute);
            finalCalendar.set(Calendar.SECOND, 0);
            finalCalendar.set(Calendar.MILLISECOND, 0);

            selectedServiceDate = finalCalendar.getTime();

            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
            binding.editTextPostServiceServiceDate.setText(formatter.format(selectedServiceDate));

            binding.inputLayoutPostServiceServiceDate.setError(null);
            binding.inputLayoutPostServiceServiceDate.setErrorEnabled(false);
        });

        timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
    }

    /**
     * Validates inputs and saves changes to Firestore.
     */
    private void saveChanges() {
        String title = getSafeText(binding.editTextPostServiceTitle.getText());
        String description = getSafeText(binding.editTextPostServiceDescription.getText());
        String requirements = getSafeText(binding.editTextPostServiceRequirements.getText());
        String volunteersNeededStr = getSafeText(binding.editTextPostServiceVolunteersNeeded.getText());
        String contact = getSafeText(binding.editTextPostServiceContactNumber.getText());

        // Validation
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
        if (TextUtils.isEmpty(volunteersNeededStr)) {
            binding.inputLayoutPostServiceVolunteersNeeded.setError("Volunteers needed is required");
            return;
        }
        if (TextUtils.isEmpty(contact)) {
            binding.inputLayoutPostServiceContactNumber.setError("Contact number is required");
            return;
        }
        if (selectedServiceDate == null) {
            binding.inputLayoutPostServiceServiceDate.setError("Service date is required");
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

        // Update service in Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("requirements", requirements);
        updates.put("volunteersNeeded", volunteersNeeded);
        updates.put("serviceDate", selectedServiceDate);
        updates.put("searchTitle", title.toLowerCase());
        updates.put("contact", contact);
        Log.d("Qimin_Debug", "Saving contact update: " + contact);

        db.collection("services").document(serviceId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Service updated successfully: " + serviceId);
                    Toast.makeText(this, "Service updated", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating service", e);
                    Toast.makeText(this, "Error updating service: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Safely gets text from an EditText, trims it, and handles nulls.
     */
    private String getSafeText(android.text.Editable editable) {
        return (editable == null) ? "" : editable.toString().trim();
    }
}

