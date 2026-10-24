-- Add Google OAuth fields to profiles table
-- This allows tracking which authentication provider a user signed up with
-- and storing the Google provider ID for account linking

ALTER TABLE profiles 
ADD COLUMN IF NOT EXISTS auth_provider TEXT DEFAULT 'EMAIL',
ADD COLUMN IF NOT EXISTS google_provider_id TEXT,
ADD COLUMN IF NOT EXISTS google_email TEXT;

-- Add index for google provider ID lookups
CREATE INDEX IF NOT EXISTS idx_profiles_google_provider_id ON profiles(google_provider_id);
CREATE INDEX IF NOT EXISTS idx_profiles_google_email ON profiles(google_email);

-- Add comment to document the purpose
COMMENT ON COLUMN profiles.auth_provider IS 'Authentication provider: EMAIL, GOOGLE, etc.';
COMMENT ON COLUMN profiles.google_provider_id IS 'Google OAuth provider subject ID for account linking';
COMMENT ON COLUMN profiles.google_email IS 'Google account email (verified) for account linking';
