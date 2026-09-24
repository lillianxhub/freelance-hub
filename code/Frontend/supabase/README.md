# Supabase setup

1. Create a Supabase project.
2. Run `migrations/202609250001_initial_schema.sql` in the SQL editor, or link a local Supabase CLI project and run `supabase db push`.
3. Copy `.env.example` to `.env.local`.
4. Set `VITE_SUPABASE_URL`, `VITE_SUPABASE_PUBLISHABLE_KEY`, and `VITE_DEMO_MODE=false`.
5. Enable email/password authentication in Supabase Auth.

The browser uses only the publishable/anon key. Never place a service-role key in a Vite environment variable. Every business table has Row Level Security policies that limit rows to `owner_id = auth.uid()`.
