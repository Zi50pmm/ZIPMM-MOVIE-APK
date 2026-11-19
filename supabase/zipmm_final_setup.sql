-- ===== supabase/zipmm_production_upgrade.sql =====
-- ZIPMM MOVIE production workflow upgrade
-- Run once in Supabase SQL Editor after the previous ZIPMM SQL files.

alter table public.zipmm_movies
  add column if not exists is_trending boolean not null default false;

-- New uploads must wait for owner/admin approval.
alter table public.zipmm_movies
  alter column is_published set default false;

-- Existing published catalogue remains published; no rows are unpublished here.
update public.zipmm_movies set is_trending = false where is_trending is null;

-- VIP transfer reference: exactly five digits. Keep old rows intact; enforce on new writes.
alter table public.zipmm_vip_payments
  alter column receipt_path set default 'manual-reference';

alter table public.zipmm_vip_payments
  drop constraint if exists zipmm_transaction_reference_five_digits;

alter table public.zipmm_vip_payments
  add constraint zipmm_transaction_reference_five_digits
  check (transaction_reference ~ '^[0-9]{5}$') not valid;

-- In-app support requests and priority support queue.
create table if not exists public.zipmm_support_tickets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  subject text not null,
  message text not null,
  priority boolean not null default false,
  status text not null default 'open' check (status in ('open','in_progress','closed')),
  admin_reply text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
alter table public.zipmm_support_tickets enable row level security;
drop policy if exists "users create own support tickets" on public.zipmm_support_tickets;
create policy "users create own support tickets"
on public.zipmm_support_tickets for insert to authenticated
with check (auth.uid() = user_id);
drop policy if exists "users read own tickets and admins read all" on public.zipmm_support_tickets;
create policy "users read own tickets and admins read all"
on public.zipmm_support_tickets for select to authenticated
using (auth.uid() = user_id or public.zipmm_is_admin());
grant select, insert on public.zipmm_support_tickets to authenticated;

-- Owner-managed payment instructions (wallet numbers/names are configured in Owner Settings).
insert into public.zipmm_app_settings(setting_key,setting_value)
values
 ('kbzpay_account','{"number":"","name":""}'::jsonb),
 ('wavepay_account','{"number":"","name":""}'::jsonb),
 ('priority_support_enabled','true'::jsonb)
on conflict(setting_key) do nothing;


-- ===== supabase/zipmm_public_payment_settings.sql =====
-- Allow the app to display only public payment instructions to non-admin users.
drop policy if exists "public reads payment instructions" on public.zipmm_app_settings;
create policy "public reads payment instructions"
on public.zipmm_app_settings for select to anon, authenticated
using (setting_key in ('kbzpay_account','wavepay_account'));
grant select on public.zipmm_app_settings to anon, authenticated;


-- ===== supabase/zipmm_support_priority_guard.sql =====
-- ZIPMM Support priority protection and admin queue permissions.
alter table public.zipmm_support_tickets enable row level security;

drop policy if exists "admins update support tickets" on public.zipmm_support_tickets;
create policy "admins update support tickets"
on public.zipmm_support_tickets for update to authenticated
using (public.zipmm_is_admin())
with check (public.zipmm_is_admin());

create or replace function public.zipmm_check_priority_support_vip()
returns trigger language plpgsql security definer
set search_path = public
as $$
begin
  if new.priority and not exists (
    select 1 from public.zipmm_vip_memberships
    where user_id = new.user_id and expires_at > now()
  ) then
    raise exception 'priority_support_requires_vip';
  end if;
  new.updated_at := now();
  return new;
end;
$$;

drop trigger if exists zipmm_support_priority_vip_guard on public.zipmm_support_tickets;
create trigger zipmm_support_priority_vip_guard
before insert or update of priority, user_id on public.zipmm_support_tickets
for each row execute function public.zipmm_check_priority_support_vip();

grant update on public.zipmm_support_tickets to authenticated;
