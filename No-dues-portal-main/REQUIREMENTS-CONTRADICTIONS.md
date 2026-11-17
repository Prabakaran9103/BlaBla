# No Dues Portal Contradictions

This file lists only the differences between the current system and
[No dues.pdf](./No%20dues.pdf).

The earlier [REQUIREMENTS-GAP-REPORT.md](./REQUIREMENTS-GAP-REPORT.md) is not
modified. This file does not repeat the normal list of implemented features.

## Implemented by the system but not specified in the PDF

These features exist in the application, but the PDF does not clearly require
them as user-facing or technical requirements:

1. **Spring Boot SSR implementation**
   - The system uses Spring Boot controllers and Thymeleaf server-side
     rendering.
   - The PDF describes a web portal and frontend/backend layers, but does not
     require Spring Boot or Thymeleaf specifically.

2. **SQLite as the runtime database**
   - The application uses SQLite.
   - The PDF requires a relational database, but does not require SQLite
     specifically.

3. **Google OAuth login**
   - Users can log in through Google when Google OAuth is configured.
   - The PDF describes email/password login and does not specify Google OAuth.

4. **Student-only public self-registration**
   - Students can register with `@smail.iitm.ac.in`.
   - The PDF describes dynamic email-domain rules instead of this fixed
     student-only registration rule.
   - This is a later product decision and intentionally replaces the PDF rule.

5. **Admin-created Staff and Faculty accounts**
   - Admin can add Staff and Faculty users.
   - The PDF describes user management but does not clearly specify this exact
     account-creation flow.

6. **Display-picture upload**
   - Users can upload and change a display picture.
   - The PDF mentions user profiles, but does not clearly require picture
     upload and storage.

7. **Password reset by email**
   - The system has a forgot-password and reset-password flow.
   - The PDF mentions password handling but does not define this complete reset
     flow as a detailed requirement.

8. **Protected single Admin account behavior**
   - The system prevents changing or permanently removing the only Admin
     account.
   - The PDF says there must be one Admin, but does not describe all of these
     UI and protection rules.

9. **Email delivery safety for inactive users**
   - Normal emails and reminders are skipped for inactive users.
   - This safety rule is not specified in the PDF.

10. **Responsive and polished visual styling**
    - The system includes shared responsive navigation and professional CSS.
    - The PDF gives frontend structure guidance but does not require this
      specific visual design.

## Specified in the PDF but not fully implemented or intentionally different

1. **Dynamic email-domain rules**
   - PDF sections 3.2, 3.3, and 12.2 require configurable domain rules that
     determine registration and default user type.
   - The system removed this component and uses student-only registration plus
     Admin-created Staff and Faculty accounts.
   - This is an intentional change requested after the PDF.

2. **Complete Faculty-list editor**
   - PDF section 5.3 requires Staff to search Faculty, add and remove Faculty,
     and reorder the list using drag and drop.
   - The system supports selecting Faculty and storing display order, but does
     not provide the complete drag-and-drop editing experience.

3. **Editing request setup after review**
   - The PDF describes Staff management of the Faculty list during the
     workflow.
   - The system does not fully support changing all request setup details
     after rejection or after the approval stage.

4. **Certificate number**
   - PDF sections 1.2, 8.2, and 8.3 require an optional certificate number and
     a certificate file copy when Staff closes a request.
   - The system supports certificate file upload, but certificate-number
     persistence and the complete close form are not fully implemented.

5. **Viewing other user profiles**
   - PDF section 3.5 says users can view user profiles.
   - The system provides the current user's profile, but not a complete
     profile-view page for other users.

6. **Notification settings screen**
   - PDF sections 11.3 and 12.3 require users to control email notification
     settings.
   - The setting exists in the data model and service, but a complete
     user-facing settings screen is not fully exposed.

7. **PDF email recipients are broader than the current policy**
   - PDF section 11.2 requires email for:
     - New request to all Staff.
     - Staff forwarding to selected Faculty.
     - Faculty rejection to the Student.
     - Student resubmission to selected Faculty.
     - Full approval to the Student and assigned Staff.
     - Staff closure to the Student.
     - Staff deletion to all associated users.
   - The current product policy intentionally sends normal workflow email only
     for:
     - Full approval to the Student.
     - Forgot-password and unread-reminder emails.
   - Other workflow events remain in-app notifications.

8. **Staff recipient on full approval**
   - The PDF requires both the Student and assigned Staff member to receive
     the full-approval email.
   - The current implementation sends that event email only to the Student.

9. **All associated users on Staff deletion**
   - The PDF requires Staff deletion email to all associated users.
   - The current implementation sends an in-app notification only, by later
     product decision.

10. **New-request email**
    - The PDF requires an email to all Staff when a new request arrives.
    - The current implementation sends an in-app notification only, by later
      product decision.

11. **Admin request-management scope**
    - PDF section 12.4 describes Admin request management and deletion.
    - The current system supports Admin deletion, but some broader Admin
      request-management views and operations are not fully equivalent to the
      PDF description.

12. **Testing levels**
    - PDF section 16 requires unit, integration, and end-to-end testing.
    - The project has service and Spring integration tests, but does not yet
      have complete browser-level end-to-end coverage for every workflow and
      email-recipient rule.

## Important interpretation

The first list contains extra implementation decisions. The second list
contains real gaps or intentional changes. A feature in the first list is not
automatically a problem; it is listed because it is not explicitly required
by the PDF.
