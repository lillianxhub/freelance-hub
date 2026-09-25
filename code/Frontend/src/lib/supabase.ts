import { createClient, type SupabaseClient } from '@supabase/supabase-js'

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL?.trim()
const supabaseKey = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY?.trim()

export const isSupabaseConfigured = Boolean(supabaseUrl && supabaseKey)
export const isDemoMode = import.meta.env.VITE_DEMO_MODE !== 'false' || !isSupabaseConfigured

export const supabase = isSupabaseConfigured
  ? createClient(supabaseUrl!, supabaseKey!, {
      auth: {
        autoRefreshToken: true,
        persistSession: true,
        detectSessionInUrl: true,
      },
    })
  : null

export function getSupabaseClient(): SupabaseClient {
  if (!supabase) throw new Error('Supabase ยังไม่ได้ตั้งค่า กรุณาตรวจสอบ environment variables')
  return supabase
}
