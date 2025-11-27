# VolunHub

VolunHub is a centralized Android platform designed to connect student volunteers with non-profit organizations, facilitating meaningful community service engagement.

## Features

### For Students
* **Service Discovery:** Browse and search for volunteer opportunities with real-time filtering capabilities.
* **One-Click Application:** Streamlined application process for volunteer services.
* **Application Tracking:** Monitor the status of applications (Pending, Accepted, Rejected) in real-time.
* **History & Saved:** Maintain a record of completed services and bookmark opportunities for future reference.
* **Profile Management:** Manage personal details, volunteer experience, and self-introduction.

### For Organizations
* **Dashboard:** View key statistics and new applicant notifications at a glance.
* **Service Management:** Create, post, and manage volunteer opportunities.
* **Applicant Review:** Review student applicants with options to accept or reject candidates.
* **Profile Management:** Showcase organization details, mission, and branding.

## Technical Stack

* **Language:** Java
* **UI/UX:** XML, Material Design 3, ConstraintLayout
* **Architecture:** Single-Activity Architecture (Fragments + Jetpack Navigation Component)
* **Binding:** View Binding
* **Backend:** Firebase Authentication, Cloud Firestore
* **Media Storage:** Cloudinary (Profile Image Management)
* **Build System:** Gradle (Kotlin DSL)

## Project Structure

The application follows a feature-based package structure to ensure modularity and maintainability:

* `com.example.volunhub`
    * `auth/` - Handles user authentication logic, including Login and Sign Up flows.
    * `student/` - Contains UI and logic for the Student experience (Home, Application Lists, Profile).
    * `org/` - Contains UI and logic for the Organization experience (Dashboard, Service Posting, Applicant Management).
    * `models/` - Data models (POJOs) representing Firestore documents (Service, Application, User, Applicant).
    * **Foundational Classes:**
        * `MyApplication` - The app entry point; handles global library initialization (Cloudinary).
        * `MainActivity` - The launcher activity; acts as a splash screen to route users based on auth state.
        * `BaseRouterActivity` - Abstract base class handling common authentication routing and navigation logic.
        * `Constants` - Centralized file for static data (e.g., Organization Fields).