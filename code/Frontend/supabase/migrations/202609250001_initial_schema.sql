-- Freelance Hub schema for Supabase Postgres.
-- Run with `supabase db push` or paste into the Supabase SQL editor.

create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  owner_id uuid not null unique default auth.uid() references auth.users(id) on delete cascade,
  full_name text not null default '',
  email text not null default '',
  phone text,
  address text,
  tax_id text,
  logo_url text,
  bank_name text,
  bank_account_name text,
  bank_account_number text,
  timezone text not null default 'Asia/Bangkok',
  currency char(3) not null default 'THB',
  date_format text not null default 'DD/MM/YYYY',
  default_tax_rate numeric(5,2) not null default 7 check (default_tax_rate between 0 and 100),
  default_hourly_rate numeric(14,2) not null default 0 check (default_hourly_rate >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.clients (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null,
  company_name text,
  email text,
  phone text,
  address text,
  tax_id text,
  notes text,
  status text not null default 'ACTIVE' check (status in ('ACTIVE', 'ARCHIVED')),
  color text not null default '#3867f4',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.projects (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  client_id uuid not null references public.clients(id) on delete restrict,
  name text not null,
  description text,
  color text not null default '#3867f4',
  status text not null default 'PLANNED' check (status in ('PLANNED', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'ARCHIVED')),
  billing_type text not null default 'HOURLY' check (billing_type in ('HOURLY', 'FIXED_PRICE')),
  hourly_rate numeric(14,2) check (hourly_rate is null or hourly_rate >= 0),
  fixed_price numeric(14,2) check (fixed_price is null or fixed_price >= 0),
  budget_hours numeric(10,2) check (budget_hours is null or budget_hours >= 0),
  budget_amount numeric(14,2) check (budget_amount is null or budget_amount >= 0),
  currency char(3) not null default 'THB',
  start_date date,
  end_date date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (end_date is null or start_date is null or end_date >= start_date),
  check (
    (billing_type = 'HOURLY' and hourly_rate is not null)
    or (billing_type = 'FIXED_PRICE' and fixed_price is not null)
  )
);

create table if not exists public.tasks (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  project_id uuid not null references public.projects(id) on delete cascade,
  name text not null,
  description text,
  status text not null default 'TODO' check (status in ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE')),
  sort_order integer not null default 0,
  due_date date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.invoices (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  client_id uuid not null references public.clients(id) on delete restrict,
  project_id uuid references public.projects(id) on delete restrict,
  invoice_number text not null,
  issue_date date not null,
  due_date date not null,
  status text not null default 'DRAFT' check (status in ('DRAFT', 'ISSUED', 'PAID', 'OVERDUE', 'VOID')),
  currency char(3) not null default 'THB',
  subtotal numeric(14,2) not null default 0 check (subtotal >= 0),
  discount_amount numeric(14,2) not null default 0 check (discount_amount >= 0),
  tax_rate numeric(5,2) not null default 0 check (tax_rate between 0 and 100),
  tax_amount numeric(14,2) not null default 0 check (tax_amount >= 0),
  total numeric(14,2) not null default 0 check (total >= 0),
  amount_paid numeric(14,2) not null default 0 check (amount_paid >= 0),
  notes text,
  seller_snapshot jsonb,
  client_snapshot jsonb,
  issued_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (owner_id, invoice_number),
  check (due_date >= issue_date)
);

create table if not exists public.time_entries (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  project_id uuid not null references public.projects(id) on delete restrict,
  task_id uuid references public.tasks(id) on delete set null,
  invoice_id uuid references public.invoices(id) on delete set null,
  description text,
  started_at timestamptz not null,
  ended_at timestamptz,
  duration_minutes integer check (duration_minutes is null or duration_minutes > 0),
  billable boolean not null default true,
  rate_snapshot numeric(14,2) not null default 0 check (rate_snapshot >= 0),
  currency char(3) not null default 'THB',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (ended_at is null or ended_at > started_at)
);

create unique index if not exists one_running_timer_per_owner
  on public.time_entries(owner_id) where ended_at is null;

create table if not exists public.invoice_items (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  invoice_id uuid not null references public.invoices(id) on delete cascade,
  time_entry_id uuid references public.time_entries(id) on delete restrict,
  description text not null,
  quantity numeric(12,2) not null check (quantity > 0),
  unit_price numeric(14,2) not null check (unit_price >= 0),
  amount numeric(14,2) not null check (amount >= 0),
  sort_order integer not null default 0,
  created_at timestamptz not null default now()
);

drop index if exists public.one_active_invoice_per_time_entry;

create or replace function public.guard_time_entry_invoice_reuse()
returns trigger language plpgsql as $$
begin
  if new.time_entry_id is not null and exists (
    select 1
    from public.invoice_items item
    join public.invoices invoice on invoice.id = item.invoice_id
    where item.time_entry_id = new.time_entry_id
      and item.id <> new.id
      and invoice.status <> 'VOID'
  ) then
    raise exception 'time entry is already attached to an active invoice';
  end if;
  return new;
end;
$$;

drop trigger if exists guard_time_entry_invoice_reuse on public.invoice_items;
create trigger guard_time_entry_invoice_reuse
before insert or update on public.invoice_items
for each row execute function public.guard_time_entry_invoice_reuse();

create table if not exists public.payments (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  invoice_id uuid not null references public.invoices(id) on delete restrict,
  amount numeric(14,2) not null check (amount > 0),
  currency char(3) not null default 'THB',
  paid_at date not null,
  method text not null default 'BANK_TRANSFER',
  notes text,
  created_at timestamptz not null default now()
);

create table if not exists public.finance_entries (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  project_id uuid references public.projects(id) on delete set null,
  type text not null check (type in ('INCOME', 'EXPENSE')),
  category text not null,
  amount numeric(14,2) not null check (amount > 0),
  currency char(3) not null default 'THB',
  entry_date date not null,
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists clients_owner_name_idx on public.clients(owner_id, company_name);
create index if not exists projects_owner_status_idx on public.projects(owner_id, status);
create index if not exists tasks_project_sort_idx on public.tasks(project_id, sort_order);
create index if not exists time_entries_owner_started_idx on public.time_entries(owner_id, started_at desc);
create index if not exists invoices_owner_status_idx on public.invoices(owner_id, status);
create index if not exists finance_entries_owner_date_idx on public.finance_entries(owner_id, entry_date desc);

create or replace function public.set_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

do $$
declare table_name text;
begin
  foreach table_name in array array['profiles','clients','projects','tasks','time_entries','finance_entries','invoices']
  loop
    execute format('drop trigger if exists set_%I_updated_at on public.%I', table_name, table_name);
    execute format('create trigger set_%I_updated_at before update on public.%I for each row execute function public.set_updated_at()', table_name, table_name);
  end loop;
end $$;

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
  insert into public.profiles (id, owner_id, full_name, email)
  values (new.id, new.id, coalesce(new.raw_user_meta_data ->> 'full_name', ''), coalesce(new.email, ''));
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
for each row execute function public.handle_new_user();

do $$
declare table_name text;
begin
  foreach table_name in array array['profiles','clients','projects','tasks','time_entries','finance_entries','invoices','invoice_items','payments']
  loop
    execute format('alter table public.%I enable row level security', table_name);
    execute format('revoke all on table public.%I from anon', table_name);
    execute format('grant select, insert, update, delete on table public.%I to authenticated', table_name);

    execute format('drop policy if exists "owner_select" on public.%I', table_name);
    execute format('drop policy if exists "owner_insert" on public.%I', table_name);
    execute format('drop policy if exists "owner_update" on public.%I', table_name);
    execute format('drop policy if exists "owner_delete" on public.%I', table_name);

    execute format('create policy "owner_select" on public.%I for select to authenticated using ((select auth.uid()) = owner_id)', table_name);
    execute format('create policy "owner_insert" on public.%I for insert to authenticated with check ((select auth.uid()) = owner_id)', table_name);
    execute format('create policy "owner_update" on public.%I for update to authenticated using ((select auth.uid()) = owner_id) with check ((select auth.uid()) = owner_id)', table_name);
    execute format('create policy "owner_delete" on public.%I for delete to authenticated using ((select auth.uid()) = owner_id)', table_name);
  end loop;
end $$;
