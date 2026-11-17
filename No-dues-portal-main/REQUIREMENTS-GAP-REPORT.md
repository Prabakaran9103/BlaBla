# No Dues Portal Requirements Gap Report

This report compares the current application with [No dues.pdf](./No%20dues.pdf).

## Implemented

- SQLite database and Spring Boot server-side rendering.
- Student, Faculty, Staff, and single Admin roles.
- Student request creation with Faculty Advisor selection.
- Request details, status, history, versions, and file uploads.
- Student correction and resubmission after rejection.
- Staff forwarding to zero to five Faculty members.
- Parallel Faculty approval, rejection, and decision withdrawal.
- Staff ownership after forwarding.
- Only the owning Staff member can close, delete, or restore a request.
- Three-day Staff restore period.
- Admin request deletion without physical database deletion.
- Student, Faculty, Staff, and Admin dashboards.
- Role-based request visibility.
- In-app notifications with read/unread state.
- Separate unread notification and reminder counters.
- Configurable unread reminder threshold.
- Normal email preference per user.
- Email reminders for unread notifications.
- Password reset email.
- Profile name, password, and display picture update.
- Admin user creation, profile update, role update, and deactivation.
- Protected single Admin account.
- Request archive and unarchive.
- Secure request file download.
- Staff certificate upload.
- CSRF protection and BCrypt password hashing.
- Optional Google login.
- `.env` configuration and setup documentation.

## Current differences and missing requirements

### 1. Registration rules

The PDF requires configurable institute email-domain rules and allows the
registration flow to determine the initial user type from those rules.

The current application intentionally differs:

- Only `@smail.iitm.ac.in` students can self-register.
- Staff and Faculty are created by Admin.
- Domain-rule management was removed based on the later product decision.
- Staff and Faculty cannot self-register.

This is an intentional product change, not an accidental missing feature.

### 2. Staff request management

The PDF requires Staff to:

- Search Faculty members while preparing the list.
- Add and remove Faculty members interactively.
- Reorder the Faculty list with drag and drop.
- Edit request setup again after rejection or full approval.

The current application supports Faculty selection and display order, but the
complete drag-and-drop editor and all post-review setup editing are not fully
implemented.

### 3. Certificate details

The PDF allows Staff to enter an optional certificate number when closing a
request. The current application supports certificate file upload, but the
certificate number field is not fully wired into the request data model and
closure form.

### 4. User profile access

The PDF allows users to view other user profiles. The current application has
the user's own profile page, but it does not provide a complete public profile
view for other users.

### 5. Notification settings

The PDF requires users to turn normal email notifications on or off. The data
model and service support this setting, but a complete user-facing notification
settings page is not fully exposed in the navigation and profile UI.

### 6. Important email events

The current email policy is intentionally limited to:

- New request: all active Staff users.
- All selected Faculty approve: the Student.
- Staff deletes a request: the Student.
- Forgot password: the account owner.
- Unread reminder threshold: the affected user.

All other workflow updates remain in-app notifications only.

The PDF also describes some broader associated-user email cases, especially
Staff deletion. The current product decision limits Staff deletion email to
the Student. This should be reviewed if emails must also go to selected Faculty
members and the owning Staff member.

Admin deletion correctly sends no notification and no email.

### 7. Notification recipient completeness

The current application creates in-app notifications for the main request
events. Some events use a broad Staff recipient list, while the PDF describes
associated users as the Student, responsible Staff member, and selected
Faculty members. Recipient rules should be reviewed if every routine update
must be sent to every associated user.

### 8. Email delivery safety

Email delivery now checks that the target account exists and is active before
sending. Inactive users are skipped for normal events and reminders. Password
reset email requires an active account.

Old emails already sent by previous application versions cannot be recalled.
The current application will not send new workflow emails to removed or
inactive users.

### 9. Admin controls

The PDF describes configurable email-domain rules. Those controls are not
present because the current product rule is student-only self-registration and
Admin-created Staff/Faculty accounts.

The current Admin page supports user management, threshold settings, user
deactivation, and protected Admin behavior.

### 10. Test coverage

The application has service tests for registration, request workflow,
notifications, and file limits. Full browser-level acceptance tests for every
PDF requirement are still missing, especially:

- Drag-and-drop Faculty list ordering.
- Profile and notification settings screens.
- Email recipient assertions with a mail sender mock.
- Admin deletion visibility rules.
- Certificate number persistence.
