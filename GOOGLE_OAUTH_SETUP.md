# Google OAuth Setup for KisanSetu

This document explains how to configure Google OAuth for the KisanSetu application.

## Architecture Overview

KisanSetu uses **Supabase Auth** for credential management and Google OAuth integration. The implementation uses Supabase's built-in Google OAuth provider, not Spring Security OAuth2. This maintains the existing authentication architecture seamlessly.

### Authentication Flow

1. User clicks "Continue with Google" button
2. Frontend redirects to Google OAuth via Supabase Auth
3. User authenticates with Google
4. Google redirects back to Supabase callback
5. Supabase creates/updates user account with Google identity
6. Supabase issues JWT token
7. Backend validates Supabase JWT and extracts Google metadata
8. Backend synchronizes user profile with Google information
9. User is redirected to appropriate dashboard based on role

## Required Configuration

### 1. Supabase Google OAuth Setup

You need to configure Google OAuth in your Supabase project:

1. Go to your Supabase project dashboard
2. Navigate to **Authentication → Providers → Google**
3. Enable Google provider
4. Configure the following:
   - **Client ID**: Your Google OAuth client ID
   - **Client Secret**: Your Google OAuth client secret
   - **Redirect URL**: Your frontend callback URL

### 2. Google Cloud Console Setup

Create a Google OAuth 2.0 client ID:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable **Google+ API** and **Google Identity** APIs
4. Go to **Credentials → Create Credentials → OAuth client ID**
5. Choose **Web application**
6. Configure authorized redirect URIs

### 3. Environment Variables

#### Frontend (`.env.local`)
```bash
NEXT_PUBLIC_SUPABASE_URL=https://your-project-id.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=your-supabase-anon-key
NEXT_PUBLIC_API_URL=http://localhost:8080
```

#### Backend (`application.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/your_database
    username: your_username
    password: your_password

supabase:
  url: https://your-project-id.supabase.co
  jwt:
    secret: your-supabase-jwt-secret

kisan-setu:
  cors:
    allowed-origins: http://localhost:3000
```

## Redirect URIs

### Development
- **Frontend callback**: `http://localhost:3000/auth`
- **Supabase callback**: Supabase handles this automatically

### Production
- **Frontend callback**: `https://your-frontend-domain.com/auth`
- **Supabase callback**: Supabase handles this automatically

## Google Console Redirect URI Configuration

When configuring the Google OAuth client, add these authorized redirect URIs:

For development:
```
https://your-project-id.supabase.co/auth/v1/callback
```

For production:
```
https://your-project-id.supabase.co/auth/v1/callback
```

Note: The redirect URI is handled by Supabase, not your application directly. The Supabase callback URI format is always:
`https://{project-id}.supabase.co/auth/v1/callback`

## Database Changes

The implementation adds the following fields to the `profiles` table:

- `auth_provider` (TEXT): Stores the authentication provider ('EMAIL', 'GOOGLE', etc.)
- `google_provider_id` (TEXT): Google OAuth provider subject ID for account linking
- `google_email` (TEXT): Google account email (verified) for account linking

These are added via the Flyway migration: `V4__add_google_auth_fields.sql`

## Account Linking Logic

The system handles account linking as follows:

1. **New Google User**: Creates a new KisanSetu account with Google identity
2. **Existing Email User + Google**: Links Google identity to existing account if email matches
3. **Role Preservation**: Google authentication never changes a user's existing role
4. **Profile Updates**: Google profile information (name, avatar) is updated on first login only

## Role Selection for New Users

For new users signing up with Google:

1. User selects role (Farmer, Merchant, Customer, Logistics) on signup page
2. Role is stored in Supabase user metadata during OAuth flow
3. Backend extracts role from JWT metadata and assigns it to the user
4. User is redirected to appropriate dashboard based on assigned role

## Security Considerations

- Google client secret is stored in Supabase, never exposed to frontend
- Backend validates Supabase JWT before trusting any user data
- Google identity information is extracted from verified JWT claims, not frontend input
- Existing RBAC and security rules are preserved
- No credentials are committed to git

## Testing

### Manual Testing Steps

1. Configure Supabase Google OAuth with test credentials
2. Start backend: `cd backend && mvn spring-boot:run`
3. Start frontend: `cd frontend && npm run dev`
4. Navigate to `http://localhost:3000/auth`
5. Test Google login on both Sign In and Sign Up tabs
6. Verify role-based redirects work correctly
7. Check database for correct Google metadata storage

### Expected Behavior

- **Sign In with Google**: Existing users can log in with Google
- **Sign Up with Google**: New users select role and create account
- **Account Linking**: Same email links Google to existing account
- **Role Preservation**: Existing roles are never changed by Google login
- **Profile Updates**: Google name/avatar updated on first login only

## Troubleshooting

### Google Login Not Working
- Verify Supabase Google provider is enabled
- Check Google Console redirect URIs match Supabase callback URL
- Ensure Supabase URL and keys are correct in frontend `.env.local`

### Account Not Created
- Check backend logs for synchronization errors
- Verify database migration `V4__add_google_auth_fields.sql` ran successfully
- Ensure user metadata contains role information for new users

### Role Not Assigned
- Verify role is passed in OAuth metadata for new users
- Check `UserRegistrationSyncService` logs for role provisioning
- Ensure role is one of: farmer, merchant, customer, logistics

## Files Modified

### Backend
- `backend/src/main/resources/db/migration/V4__add_google_auth_fields.sql` - Database migration
- `backend/src/main/java/com/kisansetu/user/entity/Profile.java` - Added Google auth fields
- `backend/src/main/java/com/kisansetu/user/repository/ProfileRepository.java` - Added Google lookup methods
- `backend/src/main/java/com/kisansetu/user/service/UserRegistrationSyncService.java` - Enhanced to handle Google metadata
- `backend/src/main/java/com/kisansetu/security/JwtAuthenticationFilter.java` - Extracts Google metadata from JWT

### Frontend
- `frontend/src/components/GoogleButton.tsx` - New Google OAuth button component
- `frontend/src/app/auth/page.tsx` - Added Google OAuth flow and UI
- `frontend/src/lib/auth.tsx` - No changes required (uses existing Supabase auth)

## Support

For issues with:
- **Supabase Auth**: Check Supabase documentation and dashboard logs
- **Google OAuth**: Check Google Cloud Console and OAuth playground
- **Backend**: Check Spring Boot logs and database connectivity
- **Frontend**: Check browser console and network requests
