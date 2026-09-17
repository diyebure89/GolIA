# Requirements Document: Football Prediction App (GolIA)

## Introduction

### 1.1 Purpose

This requirements document specifies the functional and non-functional requirements for the GolIA Football Prediction App, an Android-native mobile application that enables users to predict football match outcomes, compete in leaderboards, and stay updated with football news. The document serves as the authoritative specification for development, testing, and validation of the application.

### 1.2 Scope

The GolIA Football Prediction App provides football enthusiasts with a comprehensive platform for match predictions, leaderboard competition, and news consumption. The application integrates with external football data APIs to provide real-time match information, implements a points-based prediction scoring system, and maintains user rankings based on prediction accuracy. The app features a modern, dark-themed user interface with gold accent colors following Material Design 3 guidelines.

### 1.3 Definitions, Acronyms, and Abbreviations

This document uses the following terms consistently throughout. All terms are defined in the Glossary section.

- **API**: Application Programming Interface
- **JWT**: JSON Web Token for authentication
- **SDK**: Software Development Kit
- **UI**: User Interface
- **UX**: User Experience
- **MVVM**: Model-View-ViewModel architecture pattern
- **Clean Architecture**: Software architecture emphasizing separation of concerns

### 1.4 References

The following documents inform this requirements specification:

- Design Document: `.kiro/specs/football-prediction-app/design.md`
- Android Material Design 3 Guidelines
- IEEE 830-1998 Standard for Software Requirements Specifications

## Glossary

- **App**: The GolIA Football Prediction App mobile application
- **Match**: A scheduled football game between two teams with a defined competition
- **Prediction**: A user-submitted forecast for a match outcome (home win, draw, or away win)
- **Points**: Numerical score awarded for correct predictions based on accuracy and odds
- **Ranking**: Ordered list of users sorted by total points and accuracy percentage
- **Competition**: Football league or tournament (e.g., La Liga, Champions League)
- **Session**: Authenticated period of user activity within the application
- **Access Token**: JWT token validating authenticated API requests
- **Refresh Token**: Long-lived token used to obtain new access tokens
- **Match Status**: Current state of a match (SCHEDULED, LIVE, FINISHED, POSTPONED, CANCELLED)
- **Prediction Window**: Time period during which predictions can be submitted for a match
- **User Profile**: Authenticated user's account information and statistics
- **News Article**: Football-related news content from integrated news sources
- **Leaderboard**: Visual representation of user rankings
- **Streak**: Consecutive days of user activity within the application

## Functional Requirements

### Requirement 1: User Authentication

**User Story:** As a football fan, I want to create an account and authenticate securely, so that I can access personalized prediction features and track my performance.

#### Acceptance Criteria

1. WHEN a user enters valid registration credentials, THE App SHALL create a new account and store user data securely.
2. WHEN a user enters valid login credentials, THE App SHALL authenticate the user and provide access to authenticated features.
3. WHEN a user's access token expires, THE App SHALL attempt to refresh the token using the refresh token.
4. WHEN token refresh fails or no valid session exists, THE App SHALL redirect the user to the login screen.
5. WHEN a user initiates logout, THE App SHALL clear all stored credentials and return to the unauthenticated state.
6. THE App SHALL encrypt all authentication tokens using Android Keystore.
7. THE App SHALL enforce session timeout after 30 minutes of inactivity.

### Requirement 2: User Registration

**User Story:** As a new user, I want to register for an account with my details, so that I can create a personalized profile and start making predictions.

#### Acceptance Criteria

1. WHEN a user submits the registration form with valid data, THE App SHALL create a new user account.
2. WHEN a user submits the registration form with invalid data, THE App SHALL display specific validation errors for each field.
3. WHEN a user submits a username that already exists, THE App SHALL display "Username already taken" error.
4. WHEN a user submits an email that already exists, THE App SHALL display "Email already registered" error.
5. WHEN a user submits a weak password, THE App SHALL display password strength requirements.
6. THE App SHALL require username length between 3 and 20 characters.
7. THE App SHALL validate email format before submission.
8. THE App SHALL hash passwords using bcrypt or Argon2 before storage.

### Requirement 3: Match Display

**User Story:** As a user, I want to view upcoming football matches with team information, so that I can identify matches to predict.

#### Acceptance Criteria

1. WHEN the home screen loads, THE App SHALL display a list of upcoming matches sorted by scheduled datetime.
2. WHEN a match card is displayed, THE App SHALL show home team logo, name, away team logo, name, and scheduled time.
3. WHEN a match card is displayed, THE App SHALL show the competition name or logo.
4. WHEN a user taps a match card, THE App SHALL navigate to the match detail screen.
5. WHEN a match status changes to LIVE, THE App SHALL update the match card display accordingly.
6. WHEN a match status changes to FINISHED, THE App SHALL update the match card to show final score.
7. THE App SHALL lazy-load match cards as the user scrolls through the list.
8. THE App SHALL cache team logos locally for offline display.

### Requirement 4: Match Prediction Submission

**User Story:** As a user, I want to submit predictions for football matches, so that I can compete for points and improve my ranking.

#### Acceptance Criteria

1. WHEN a user navigates to a scheduled match, THE App SHALL display three prediction options: Home Win, Draw, Away Win.
2. WHEN a user selects a prediction option, THE App SHALL highlight the selection with gold border styling.
3. WHEN a user confirms a selection before the match start time, THE App SHALL submit the prediction and store it.
4. WHEN a user attempts to predict a match that has started, THE App SHALL display "Predictions are closed for this match" message.
5. WHEN a user attempts to submit a second prediction for the same match, THE App SHALL display "You have already predicted this match" message.
6. WHEN a prediction is submitted successfully, THE App SHALL provide visual confirmation and return to the previous screen.
7. THE App SHALL prevent predictions from being submitted after the match scheduled datetime.
8. THE App SHALL display odds information for each prediction outcome.

### Requirement 5: Points Calculation

**User Story:** As a user, I want to earn points for correct predictions, so that I can track my performance and compete with other users.

#### Acceptance Criteria

1. WHEN a match finishes, THE App SHALL calculate points for all submitted predictions based on the actual outcome.
2. THE App SHALL award points according to the formula: points_earned = calculatePoints(predicted_outcome, actual_outcome, odds).
3. WHEN a user's prediction matches the actual outcome, THE App SHALL award positive points up to the maximum available.
4. WHEN a user's prediction does not match the actual outcome, THE App SHALL award zero points.
5. THE App SHALL ensure points_earned is always between 0 and MAX_POINTS.
6. WHEN points are calculated, THE App SHALL update the user's total_points in the database.
7. THE App SHALL update prediction.is_correct boolean based on outcome comparison.

### Requirement 6: Rankings Display

**User Story:** As a user, I want to view user rankings, so that I can see how I compare with other prediction enthusiasts.

#### Acceptance Criteria

1. WHEN a user navigates to the rankings screen, THE App SHALL display a leaderboard sorted by total_points.
2. WHEN users have equal total_points, THE App SHALL sort by accuracy_percentage as tiebreaker.
3. THE App SHALL display the top three ranks with gold, silver, and bronze visual distinction.
4. THE App SHALL display each ranking entry with user avatar, username, total_points, and rank position.
5. WHEN the user's entry is visible, THE App SHALL highlight the current user's entry with gold background styling.
6. THE App SHALL display rank change indicators (up arrow, down arrow, or unchanged) where available.
7. THE App SHALL support filtering by time period (daily, weekly, monthly, all-time).
8. THE App SHALL refresh ranking data in the background every 30 seconds during active viewing.

### Requirement 7: User Profile

**User Story:** As an authenticated user, I want to view my profile and statistics, so that I can track my prediction performance over time.

#### Acceptance Criteria

1. WHEN a user navigates to the profile screen, THE App SHALL display user avatar, username, and optional bio.
2. THE App SHALL display statistics including total_predictions, accuracy_percentage, total_points, and current rank.
3. THE App SHALL calculate accuracy_percentage as (predictions_correct / predictions_made) * 100.
4. THE App SHALL display current streak days (consecutive days with activity).
5. THE App SHALL display rank for daily, weekly, and all-time periods.
6. WHEN a user views their profile, THE App SHALL show achievement badges earned.
7. THE App SHALL provide access to account settings from the profile screen.
8. THE App SHALL provide access to prediction history from the profile screen.

### Requirement 8: News Display

**User Story:** As a football fan, I want to read football news articles, so that I stay informed about the latest updates and transfers.

#### Acceptance Criteria

1. WHEN a user navigates to the news section, THE App SHALL display a list of football news articles.
2. THE App SHALL display each news article with title, thumbnail image, source, and published timestamp.
3. THE App SHALL display a category badge for each article (TRANSFER, MATCH_REPORT, INTERVIEW, GENERAL).
4. WHEN a user taps a news article, THE App SHALL open the full article content or external link.
5. THE App SHALL support bookmarking articles for later reading.
6. THE App SHALL display bookmarked articles with a filled bookmark icon.
7. THE App SHALL filter news by category based on user preferences.
8. THE App SHALL lazy-load article images as users scroll through the list.

### Requirement 9: Prediction History

**User Story:** As a user, I want to view my past predictions and their results, so that I can review my performance and learn from outcomes.

#### Acceptance Criteria

1. WHEN a user accesses prediction history, THE App SHALL display a list of all submitted predictions.
2. THE App SHALL display each prediction with match information, selected outcome, points earned, and correctness status.
3. THE App SHALL sort prediction history by created_at in descending order (newest first).
4. THE App SHALL indicate correct predictions with green styling and incorrect with red styling.
5. THE App SHALL allow filtering by prediction status (correct, incorrect, pending).
6. THE App SHALL allow filtering by date range.
7. THE App SHALL display pending predictions (matches not yet played) with appropriate status.

### Requirement 10: Offline Support

**User Story:** As a user with intermittent connectivity, I want to access cached match and prediction data offline, so that I can use basic features without internet connection.

#### Acceptance Criteria

1. WHEN network connectivity is unavailable, THE App SHALL display cached match data.
2. WHEN network connectivity is unavailable, THE App SHALL display cached prediction history.
3. THE App SHALL store recently viewed matches locally using Room database.
4. THE App SHALL store user predictions locally for offline access.
5. WHEN a prediction is submitted offline, THE App SHALL queue the submission for background sync.
6. THE App SHALL attempt to sync queued predictions when network connectivity is restored.
7. THE App SHALL display "Last updated [timestamp]" indicator when showing cached ranking data.
8. THE App SHALL notify the user when pending predictions are successfully synced.

### Requirement 11: Notification System

**User Story:** As a user, I want to receive notifications about match starts, results, and ranking changes, so that I stay engaged with the app and never miss important updates.

#### Acceptance Criteria

1. WHEN a user enables notifications, THE App SHALL send reminders before matches the user has predicted.
2. WHEN a match finishes and the user's prediction is scored, THE App SHALL notify the user of the result.
3. WHEN the user's ranking changes significantly, THE App SHALL notify the user.
4. THE App SHALL respect user notification preferences for different notification types.
5. THE App SHALL allow users to configure notification timing (e.g., 1 hour before match).
6. THE App SHALL not send notifications during silent hours configured by the user.
7. THE App SHALL support deep linking from notifications to relevant screens.

### Requirement 12: Search and Filter

**User Story:** As a user, I want to search for specific teams, competitions, or matches, so that I can quickly find relevant content.

#### Acceptance Criteria

1. WHEN a user enters search text, THE App SHALL filter matches containing the search term in team names.
2. THE App SHALL display search results in real-time as the user types.
3. THE App SHALL allow filtering matches by competition.
4. THE App SHALL allow filtering matches by date range.
5. THE App SHALL allow filtering news by category.
6. THE App SHALL display "No results found" when search yields no matches.
7. THE App SHALL clear search results when search text is cleared.

### Requirement 13: Account Management

**User Story:** As an authenticated user, I want to manage my account settings, so that I can customize my app experience and keep my information current.

#### Acceptance Criteria

1. WHEN a user accesses account settings, THE App SHALL display options for profile editing.
2. THE App SHALL allow users to update their username within length constraints.
3. THE App SHALL allow users to update their avatar image.
4. THE App SHALL allow users to update their country selection.
5. THE App SHALL allow users to change their password with current password verification.
6. THE App SHALL allow users to enable or disable notification preferences.
7. THE App SHALL allow users to delete their account with confirmation.
8. THE App SHALL require re-authentication for sensitive account changes.

## Non-Functional Requirements

### Performance Requirements

1. THE App SHALL load the home screen with matches within 2 seconds on standard networks.
2. THE App SHALL respond to user interactions within 100 milliseconds.
3. THE App SHALL support pagination with 20-50 items per page for list displays.
4. THE App SHALL cache images locally to prevent repeated network requests.
5. THE App SHALL use lazy loading for match cards and news articles in scrollable lists.
6. THE App SHALL complete background data refresh within 5 seconds.
7. THE App SHALL maintain responsive UI during network operations using coroutines.
8. THE App SHALL limit memory usage to prevent crashes on devices with limited RAM.

### Security Requirements

1. THE App SHALL encrypt all authentication tokens using Android Keystore.
2. THE App SHALL use HTTPS with TLS 1.2 minimum for all API communications.
3. THE App SHALL implement certificate pinning for API communications.
4. THE App SHALL validate all user inputs client-side before submission.
5. THE App SHALL hash passwords using bcrypt or Argon2 before storage.
6. THE App SHALL enforce automatic logout after 30 minutes of inactivity.
7. THE App SHALL require biometric confirmation for sensitive operations.
8. THE App SHALL obfuscate code in release builds using Proguard/R8.
9. THE App SHALL not store sensitive data in SharedPreferences in plain text.

### Usability Requirements

1. THE App SHALL follow Material Design 3 guidelines for visual design.
2. THE App SHALL use dark theme with gold (#FFD700) accent colors.
3. THE App SHALL display text in high contrast for readability.
4. THE App SHALL support screen sizes from 4.7 inches to 12.9 inches.
5. THE App SHALL support portrait and landscape orientations.
6. THE App SHALL provide clear visual feedback for all user interactions.
7. THE App SHALL display loading indicators during network operations.
8. THE App SHALL provide error messages that explain issues clearly.
9. THE App SHALL support accessibility features including screen readers.

### Compatibility Requirements

1. THE App SHALL target Android SDK 34 (Android 14).
2. THE App SHALL support minimum Android SDK 24 (Android 7.0).
3. THE App SHALL function on devices with varying screen densities (ldpi to xxxhdpi).
4. THE App SHALL support Google Play Services for authentication.
5. THE App SHALL integrate with Android Keystore for secure storage.
6. THE App SHALL not require root access or special permissions.

### Reliability Requirements

1. THE App SHALL handle network timeout errors gracefully with retry options.
2. THE App SHALL display appropriate error messages for all failure scenarios.
3. THE App SHALL maintain data consistency during app crashes.
4. THE App SHALL sync pending predictions when network is restored.
5. THE App SHALL handle API rate limiting with exponential backoff retry.
6. THE App SHALL log errors for monitoring and debugging purposes.

## Data Requirements

### Data Models

#### User Data

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| id | UUID | Required, Unique | Primary identifier |
| username | String | 3-20 chars, alphanumeric | Display name |
| email | String | Valid email format | Contact email |
| password_hash | String | bcrypt/Argon2 hash | Encrypted password |
| avatar_url | String | Valid URL | Profile image |
| country | String | Valid country code | User's country |
| created_at | DateTime | Required | Account creation time |
| total_points | Integer | ≥ 0 | Cumulative points |
| predictions_made | Integer | ≥ 0 | Total predictions count |
| predictions_correct | Integer | ≤ predictions_made | Correct predictions count |
| current_rank | Integer | ≥ 1 | Current ranking position |
| streak_days | Integer | ≥ 0 | Consecutive active days |
| is_active | Boolean | Required | Account status |

#### Match Data

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| id | UUID | Required, Unique | Primary identifier |
| external_id | String | Required | External API identifier |
| competition_id | String | Required | Competition reference |
| competition_name | String | Required | Competition display name |
| matchday | Integer | ≥ 1 | Competition matchday |
| home_team_id | UUID | Required | Home team reference |
| home_team_name | String | Required | Home team name |
| home_team_logo_url | String | Valid URL | Home team logo |
| away_team_id | UUID | Required | Away team reference |
| away_team_name | String | Required | Away team name |
| away_team_logo_url | String | Valid URL | Away team logo |
| scheduled_datetime | DateTime | Required, Future | Match schedule |
| status | String | Enum: SCHEDULED, LIVE, FINISHED, POSTPONED, CANCELLED | Current status |
| home_score | Integer | ≥ 0, Null if not finished | Final home score |
| away_score | Integer | ≥ 0, Null if not finished | Final away score |
| home_odds | Real | ≥ 0 | Home win odds |
| draw_odds | Real | ≥ 0 | Draw odds |
| away_odds | Real | ≥ 0 | Away win odds |

#### Prediction Data

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| id | UUID | Required, Unique | Primary identifier |
| user_id | UUID | Required | User reference |
| match_id | UUID | Required | Match reference |
| predicted_outcome | String | Enum: HOME_WIN, DRAW, AWAY_WIN | User's prediction |
| predicted_home_score | Integer | ≥ 0 | Optional score prediction |
| predicted_away_score | Integer | ≥ 0 | Optional score prediction |
| points_earned | Integer | ≥ 0, ≤ MAX_POINTS | Points awarded |
| is_correct | Boolean | Null if pending | Prediction result |
| created_at | DateTime | Required | Prediction time |
| finalized_at | DateTime | Null if pending | Scoring time |

### Data Storage Requirements

1. THE App SHALL use Room database for structured local data storage.
2. THE App SHALL use encrypted SharedPreferences for simple key-value pairs on API 23+.
3. THE App SHALL cache images locally using Glide or Coil image loading library.
4. THE App SHALL implement data synchronization strategy for offline support.
5. THE App SHALL maintain data integrity during app updates.

### Data Exchange Requirements

1. THE App SHALL use JSON format for API request and response bodies.
2. THE App SHALL use UTF-8 encoding for all text data.
3. THE App SHALL implement proper date/time serialization.
4. THE App SHALL handle API versioning for backward compatibility.

## Interface Requirements

### User Interface Requirements

#### Screen Layout Specifications

1. THE App SHALL display splash/login screen as initial entry point with branding and login form.
2. THE App SHALL display bottom navigation bar with 4 tabs: Inicio (Home), Partidos (Matches), Pronósticos (Predictions), Perfil (Profile).
3. THE App SHALL display active tab with gold coloring for icon and optional text label.
4. THE App SHALL use card-based layouts for match displays with rounded corners (12dp radius).
5. THE App SHALL use dark gray (#1E1E1E) background color for cards.
6. THE App SHALL use gold (#FFD700) accent color for interactive elements.

#### Match Card Interface

1. THE App SHALL display match cards with home team logo (48x48dp) on left.
2. THE App SHALL display match time in center of match card.
3. THE App SHALL display away team logo (48x48dp) on right.
4. THE App SHALL display team names below respective logos.
5. THE App SHALL display competition badge on match card.
6. THE App SHALL display "Apostar" button for scheduled matches.
7. THE App SHALL display final score for finished matches.

#### Prediction Input Interface

1. THE App SHALL display prediction options as three selectable cards (100dp width, 80dp height).
2. THE App SHALL highlight selected prediction with gold border (2dp).
3. THE App SHALL display odds below each prediction option.
4. THE App SHALL enable "Confirm Prediction" button only when selection is made.
5. THE App SHALL display "Cancel" button for abandoning prediction.

#### Ranking Interface

1. THE App SHALL display top 3 ranks with special styling (medal colors).
2. THE App SHALL display rank position with large typography (24sp for top 3).
3. THE App SHALL display user avatar (48dp for top entries).
4. THE App SHALL display points in gold color.
5. THE App SHALL highlight current user's entry with gold background tint.
6. THE App SHALL display rank change indicators.

#### Navigation Requirements

1. THE App SHALL support bottom navigation for primary section access.
2. THE App SHALL navigate to match detail when user taps a match card.
3. THE App SHALL navigate back when user presses back button.
4. THE App SHALL provide swipe navigation between tabs where appropriate.
5. THE App SHALL maintain navigation state across configuration changes.

#### Input Requirements

1. THE App SHALL display virtual keyboard for text input fields.
2. THE App SHALL move focus between fields on return key press.
3. THE App SHALL validate email format during input.
4. THE App SHALL display password field with masking.
5. THE App SHALL provide password strength indicator during registration.

## Correctness Properties

The following properties define the expected behavior of core application features and serve as formal specifications for testing.

### Property 1: User Data Consistency

*For all* active users in the system, the following invariants SHALL hold:

- `user.total_points ≥ 0`
- `user.predictions_correct ≤ user.predictions_made`
- `user.login_count ≥ 0`

**Validates: Requirements 1.1, 1.2, 5.1**

### Property 2: Prediction Validity

*For all* predictions in the system, the following invariants SHALL hold:

- `prediction.created_at < prediction.match.scheduled_datetime` (prediction submitted before match)
- `prediction.predicted_outcome ∈ {"HOME_WIN", "DRAW", "AWAY_WIN"}` (valid outcome)
- `prediction.points_earned ≥ 0 AND prediction.points_earned ≤ MAX_POINTS` (valid points)

**Validates: Requirements 4.1, 4.3, 4.7**

### Property 3: Prediction Correctness Logic

*For all* finalized predictions (where `match.status = "FINISHED"`), the following property SHALL hold:

- `prediction.is_correct = true` if and only if `prediction.predicted_outcome = actual_outcome(prediction.match)`

**Validates: Requirements 5.1, 5.2, 5.4**

### Property 4: Points Calculation Correctness

*For all* finalized predictions, the following property SHALL hold:

- `prediction.points_earned = calculatePoints(prediction.predicted_outcome, prediction.match.actual_outcome, prediction.match.odds)`

**Validates: Requirements 5.2, 5.3, 5.5**

### Property 5: Ranking Order Invariant

*For all* ranking entries and for all pairs of entries `(i, j)` where `i.rank_position < j.rank_position`:

- `i.total_points ≥ j.total_points` OR
- (`i.total_points = j.total_points` AND `i.accuracy_percentage ≥ j.accuracy_percentage`)

**Validates: Requirements 6.1, 6.2**

### Property 6: Match Status Consistency

*For all* matches in the system, the following properties SHALL hold:

- If `match.status = "FINISHED"` then `match.home_score ≠ NULL AND match.away_score ≠ NULL`
- If `match.status = "SCHEDULED"` then `match.home_score = NULL AND match.away_score = NULL`
- If `match.status = "SCHEDULED"` then `match.scheduled_datetime > NOW()`

**Validates: Requirements 3.5, 3.6, 4.4**

### Property 7: Authentication Token Security

*For all* authenticated sessions:

- Access tokens are stored encrypted using Android Keystore
- Refresh tokens are stored encrypted using Android Keystore
- Sessions expire after 30 minutes of inactivity

**Validates: Requirements 1.6, 1.7**

### Property 8: Prediction Uniqueness

*For all* users and matches:

- A user can have at most one prediction per match (`user_id, match_id` is unique)

**Validates: Requirements 4.5**

### Property 9: User Statistics Calculation

*For all* active users, the following properties SHALL hold:

- `accuracy_percentage = (predictions_correct / predictions_made) * 100`
- `average_points_per_prediction = total_points / predictions_made`

**Validates: Requirements 7.3, 7.4**

### Property 10: News Article Validity

*For all* news articles in the system:

- `article.published_at ≤ NOW()` (published articles have past timestamps)
- `article.article_url` is a valid URL format
- `article.source` references an allowed news provider
- `article.category ∈ {"TRANSFER", "MATCH_REPORT", "INTERVIEW", "GENERAL"}`

**Validates: Requirements 8.1, 8.2, 8.3**

## Testing Strategy

### Test Classification Summary

Based on the requirements analysis, the following testing strategy is recommended:

**Property-Based Tests:**
- User data consistency across all authenticated users
- Prediction validity for all prediction submissions
- Points calculation for all finalized predictions
- Ranking order for all leaderboard calculations
- Match status consistency for all match updates

**Example-Based Tests:**
- User registration with specific valid and invalid inputs
- Login authentication with specific credential combinations
- Match prediction submission for specific match scenarios
- Profile updates with specific field modifications

**Integration Tests:**
- Authentication flow with token refresh and session management
- API integration for fetching matches, predictions, and rankings
- Database operations for local data storage and caching
- Background sync for offline prediction submissions

**Smoke Tests:**
- App launch and splash screen display
- Bottom navigation functionality
- Critical user journey completion (registration through first prediction)

## Appendices

### Appendix A: Acceptance Criteria Testing Prework

| ID | Acceptance Criteria | Thoughts | Classification | Test Strategy |
|----|---------------------|----------|----------------|---------------|
| 1.1 | User registration with valid credentials | This validates that the system correctly processes valid input and creates accounts | PROPERTY | Test user creation with generated valid data |
| 1.2 | User login with valid credentials | Tests authentication flow end-to-end | PROPERTY | Test login with valid credentials and verify token storage |
| 1.3 | Token refresh on expiration | Tests automatic session maintenance | PROPERTY | Test that expired tokens trigger refresh and maintain session |
| 2.1 | Username validation (3-20 chars) | Ensures username constraints are enforced | PROPERTY | Test boundary values (2, 3, 20, 21 chars) |
| 2.2 | Email format validation | Ensures email format is checked | PROPERTY | Test valid and invalid email formats |
| 2.3 | Password strength enforcement | Ensures password requirements are checked | PROPERTY | Test weak and strong passwords |
| 3.1 | Match list display sorted by datetime | Tests data presentation order | PROPERTY | Generate random matches, verify sort order |
| 3.2 | Match card information display | Tests UI rendering correctness | PROPERTY | Generate random matches, verify all fields render |
| 3.3 | Match card navigation | Tests user interaction flow | EXAMPLE | Test navigation on single example |
| 4.1 | Prediction options display | Tests UI rendering completeness | PROPERTY | Generate random matches, verify all 3 options display |
| 4.2 | Prediction selection highlighting | Tests visual feedback | PROPERTY | Generate matches, test selection states |
| 4.3 | Prediction submission before match start | Tests timing validation | PROPERTY | Generate predictions with various timestamps, verify validation |
| 4.4 | Prediction prevention after match start | Tests deadline enforcement | PROPERTY | Generate predictions after scheduled time, verify rejection |
| 4.5 | Duplicate prediction prevention | Tests uniqueness constraint | PROPERTY | Attempt duplicate predictions, verify rejection |
| 5.1 | Points calculation on match completion | Tests scoring logic | PROPERTY | Generate predictions and matches, verify points calculation |
| 5.2 | Points range validation | Tests boundary constraints | PROPERTY | Verify points_earned is always valid range |
| 5.3 | Correctness determination | Tests outcome comparison | PROPERTY | Generate predictions, verify is_correct matches actual outcome |
| 6.1 | Ranking sort order | Tests sorting logic | PROPERTY | Generate users with various points, verify sort order |
| 6.2 | Tiebreaker logic | Tests secondary sort criteria | PROPERTY | Generate users with equal points, verify accuracy tiebreaker |
| 6.3 | Top 3 visual distinction | Tests UI rendering for special ranks | EXAMPLE | Test UI rendering for specific top 3 users |
| 6.4 | Current user highlighting | Tests user-specific UI rendering | EXAMPLE | Test highlighting for current user's entry |
| 7.1 | Profile statistics display | Tests data aggregation | PROPERTY | Generate user activity, verify statistics calculation |
| 7.2 | Accuracy calculation | Tests percentage computation | PROPERTY | Generate predictions, verify accuracy formula |
| 7.3 | Streak calculation | Tests consecutive day logic | PROPERTY | Simulate user activity patterns, verify streak counting |
| 8.1 | News article list display | Tests data presentation | PROPERTY | Generate articles, verify all fields render |
| 8.2 | Category badge display | Tests categorization UI | PROPERTY | Generate articles with various categories, verify badges |
| 8.3 | Article navigation | Tests user interaction | EXAMPLE | Test navigation on single article |
| 8.4 | Bookmark functionality | Tests state management | PROPERTY | Toggle bookmarks, verify state persistence |
| 9.1 | Prediction history display | Tests history rendering | PROPERTY | Generate predictions, verify display |
| 9.2 | History sorting | Tests temporal ordering | PROPERTY | Generate predictions, verify newest-first order |
| 9.3 | Pending vs finalized display | Tests status visualization | PROPERTY | Generate pending and finalized predictions, verify styling |
| 10.1 | Offline match display | Tests caching behavior | PROPERTY | Generate offline scenario, verify cached data display |
| 10.2 | Offline prediction queue | Tests offline submission | PROPERTY | Generate offline prediction, verify queue and sync |
| 11.1 | Match reminder notifications | Tests notification triggering | INTEGRATION | Integration test with notification system |
| 11.2 | Result notification | Tests notification content | INTEGRATION | Integration test with notification system |
| 11.3 | Notification preferences | Tests customization | EXAMPLE | Test preference saving and loading |
| 12.1 | Match search functionality | Tests text matching | PROPERTY | Generate matches and queries, verify filtering |
| 12.2 | Competition filter | Tests category filtering | PROPERTY | Generate competitions and filters, verify filtering |
| 12.3 | Search results display | Tests result presentation | PROPERTY | Generate searches, verify result display |
| 13.1 | Profile editing | Tests field updates | EXAMPLE | Test updating each editable field |
| 13.2 | Password change | Tests security workflow | EXAMPLE | Test password change flow with verification |
| 13.3 | Notification preferences | Tests preference save/load | EXAMPLE | Test preference modification |

### Appendix B: EARS Pattern Usage Summary

This requirements document uses EARS (Easy Approach to Requirements Syntax) patterns as follows:

- **Ubiquitous Requirements**: Requirements that always apply (e.g., security constraints, validation rules)
- **Event-Driven Requirements**: Requirements triggered by specific user actions (e.g., login, prediction submission)
- **State-Driven Requirements**: Requirements that apply during specific states (e.g., offline mode, authenticated session)
- **Unwanted Event Requirements**: Requirements for error handling (e.g., invalid input, network failure)
- **Complex Requirements**: Requirements combining multiple conditions (e.g., prediction validation with timing and outcome)

### Appendix C: Requirements Traceability Matrix

| Requirement | Source Design Element | Priority | Status |
|------------|----------------------|----------|--------|
| 1. User Authentication | Authentication Flow, Error Handling | High | Draft |
| 2. User Registration | Registration Screen, User Model | High | Draft |
| 3. Match Display | Home Screen, Match Card Component, Match Model | High | Draft |
| 4. Match Prediction | Match Detail Screen, Prediction Input Component | High | Draft |
| 5. Points Calculation | Prediction Model, Correctness Properties | High | Draft |
| 6. Rankings Display | Rankings Screen, Ranking Model | Medium | Draft |
| 7. User Profile | Profile Screen, User Model | Medium | Draft |
| 8. News Display | News Card Component, News Model | Medium | Draft |
| 9. Prediction History | Profile Screen, Prediction Model | Medium | Draft |
| 10. Offline Support | Data Layer, Performance Considerations | Medium | Draft |
| 11. Notification System | Profile Screen, Performance Considerations | Low | Draft |
| 12. Search and Filter | Home Screen, Performance Considerations | Medium | Draft |
| 13. Account Management | Profile Screen, Security Considerations | Medium | Draft |