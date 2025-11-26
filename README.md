# VolunHub 🤝

A centralized Android platform connecting student volunteers with non-profit organizations for meaningful community service.

## 📱 Features

### 🎓 For Students
* **Service Discovery:** Browse and search for volunteer opportunities with real-time filtering.
* **One-Click Application:** Apply to services instantly.
* **Application Tracking:** View status of applications (Pending, Accepted, Rejected) in real-time.
* **History & Saved:** Keep track of completed services and bookmark interesting opportunities.
* **Profile Management:** Manage personal details and experience.

### 🏢 For Organizations
* **Dashboard:** At-a-glance view of new applicants and key statistics.
* **Service Management:** Post new volunteer opportunities and manage existing ones.
* **Applicant Review:** Accept or reject student applicants with a simple interface.
* **Profile Management:** Showcase organization details and mission.

## 🛠️ Tech Stack

* **Language:** Java
* **UI/UX:** XML, Material Design 3, ConstraintLayout
* **Architecture:** Single-Activity Architecture (Fragments + Jetpack Navigation Component)
* **Binding:** View Binding
* **Backend:** Firebase Authentication, Cloud Firestore
* **Media Storage:** Cloudinary (for profile images)
* **Build System:** Gradle (Kotlin DSL)

## 🚀 Setup Instructions

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/leezhenxue/VolunHub.git](https://github.com/leezhenxue/VolunHub.git)
    ```

2.  **Add Configuration Files:**
    * **Firebase:** Obtain the `google-services.json` file from the Tech Lead and place it in the `app/` directory.
    * **Cloudinary:** Ensure the `CLOUDINARY_CLOUD_NAME` is configured in `MyApplication.java` (or AndroidManifest, depending on current configuration).

3.  **Build & Run:**
    * Open the project in Android Studio (Ladybug or newer recommended).
    * Sync Gradle files.
    * Run on an Emulator (API 24+) or physical device.

## 📂 Project Structure

The project follows a feature-based package structure:

* `com.example.volunhub`
    * `auth/` - Login and Sign Up logic.
    * `student/` - Student home, application list, and profile fragments.
    * `org/` - Organization dashboard, service posting, and applicant management.
    * `models/` - POJO classes for Firestore data (Service, Application, User).
    * `BaseRouterActivity` - Handles common auth routing logic.