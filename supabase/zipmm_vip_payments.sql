-- ZIPMM MOVIE manual KBZPay / WavePay payment review
-- Run once in Supabase SQL Editor as project owner.

create extension if not exists pgcrypto;

create table if not exists public.zipmm_vip_payments (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  plan text not null check (plan in ('1 Month','3 Months')),
  amount_mmk integer not null check (amount_mmk in (3000,8000)),
  payment_method text not null check (payment_method in ('KBZPay','WavePay')),
  transaction_reference text not null,
  receipt_path text not null,
  status text not null default 'pending' check (status in ('pending','approved','rejected')),
  admin_note text,
  reviewed_by uuid references auth.users(id),
  reviewed_at timestamptz,
  created_at timestamptz not null default now(),
  constraint zipmm_plan_amount check (
    (plan = '1 Month' and amount_mmk = 3000) or
    (plan = '3 Months' and amount_mmk = 8000)
  )
);

create table if not exists public.zipmm_vip_memberships (
  user_id uuid primary key references auth.users(id) on delete cascade,
  expires_at timestamptz not null,
  updated_at timestamptz not null default now()
);

create index if not exists zipmm_vip_payments_user_created
  on public.zipmm_vip_payments(user_id, created_at desc);
create index if not exists zipmm_vip_payments_status_created
  on public.zipmm_vip_payments(status, created_at desc);

create or replace function public.zipmm_is_admin()
returns boolean
language sql stable
as $$
  select coalesce((auth.jwt() -> 'app_metadata' ->> 'role') = 'admin', false);
$$;

alter table public.zipmm_vip_payments enable row level security;
alter table public.zipmm_vip_memberships enable row level security;

drop policy if exists "user submits own pending payment" on public.zipmm_vip_payments;
create policy "user submits own pending payment"
on public.zipmm_vip_payments for insert to authenticated
with check (
  auth.uid() = user_id
  and status = 'pending'
  and reviewed_by is null
  and reviewed_at is null
);

drop policy if exists "users read own payments or admins read all" on public.zipmm_vip_payments;
create policy "users read own payments or admins read all"
on public.zipmm_vip_payments for select to authenticated
using (auth.uid() = user_id or public.zipmm_is_admin());

drop policy if exists "users read own VIP or admins read all" on public.zipmm_vip_memberships;
create policy "users read own VIP or admins read all"
on public.zipmm_vip_memberships for select to authenticated
using (auth.uid() = user_id or public.zipmm_is_admin());

revoke update, delete on public.zipmm_vip_payments from anon, authenticated;
revoke insert, update, delete on public.zipmm_vip_memberships from anon, authenticated;
grant select, insert on public.zipmm_vip_payments to authenticated;
grant select on public.zipmm_vip_memberships to authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('zipmm-payment-receipts','zipmm-payment-receipts',false,5242880,
        array['image/jpeg','image/png','image/webp'])
on conflict (id) do update set public = false, file_size_limit = 5242880,
  allowed_mime_types = array['image/jpeg','image/png','image/webp'];

drop policy if exists "users upload own payment receipts" on storage.objects;
create policy "users upload own payment receipts"
on storage.objects for insert to authenticated
with check (
  bucket_id = 'zipmm-payment-receipts'
  and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "users view own receipts or admins view all" on storage.objects;
create policy "users view own receipts or admins view all"
on storage.objects for select to authenticated
using (
  bucket_id = 'zipmm-payment-receipts'
  and ((storage.foldername(name))[1] = auth.uid()::text or public.zipmm_is_admin())
);

create or replace function public.zipmm_approve_vip_payment(payment_id uuid)
returns timestamptz
language plpgsql
security definer
set search_path = public
as $$
declare
  p public.zipmm_vip_payments%rowtype;
  new_expiry timestamptz;
  months_to_add integer;
begin
  if not public.zipmm_is_admin() then
    raise exception 'admin_only';
  end if;

  select * into p from public.zipmm_vip_payments
  where id = payment_id for update;

  if not found then raise exception 'payment_not_found'; end if;
  if p.status <> 'pending' then raise exception 'payment_already_reviewed'; end if;

  months_to_add := case p.plan when '1 Month' then 1 when '3 Months' then 3 end;

  select greatest(coalesce(expires_at, now()), now()) into new_expiry
  from public.zipmm_vip_memberships where user_id = p.user_id;

  if new_expiry is null then new_expiry := now(); end if;
  new_expiry := new_expiry + make_interval(months => months_to_add);

  insert into public.zipmm_vip_memberships(user_id, expires_at, updated_at)
  values (p.user_id, new_expiry, now())
  on conflict (user_id) do update
    set expires_at = excluded.expires_at, updated_at = now();

  update public.zipmm_vip_payments
  set status = 'approved', reviewed_by = auth.uid(), reviewed_at = now()
  where id = payment_id;

  return new_expiry;
end;
$$;

create or replace function public.zipmm_reject_vip_payment(payment_id uuid, note text default null)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.zipmm_is_admin() then raise exception 'admin_only'; end if;

  update public.zipmm_vip_payments
  set status = 'rejected',
      admin_note = left(coalesce(note,''),500),
      reviewed_by = auth.uid(),
      reviewed_at = now()
  where id = payment_id and status = 'pending';

  if not found then raise exception 'payment_not_found_or_already_reviewed'; end if;
end;
$$;

revoke all on function public.zipmm_approve_vip_payment(uuid) from public, anon;
revoke all on function public.zipmm_reject_vip_payment(uuid,text) from public, anon;
grant execute on function public.zipmm_approve_vip_payment(uuid) to authenticated;
grant execute on function public.zipmm_reject_vip_payment(uuid,text) to authenticated;

-- ZIPMM MOVIE OWNER ADMIN bootstrap
-- Run this block once in Supabase SQL Editor after the owner has registered
-- and confirmed this email in Supabase Auth.
do $$
declare owner_user_id uuid;
begin
  select id into owner_user_id
  from auth.users
  where lower(email) = lower('flowerkingmyanmar@gmail.com')
  limit 1;

  if owner_user_id is null then
    raise exception 'Owner account not found. Register and confirm flowerkingmyanmar@gmail.com first.';
  end if;

  update auth.users
  set raw_app_meta_data =
    coalesce(raw_app_meta_data, '{}'::jsonb) || jsonb_build_object('role','admin')
  where id = owner_user_id;
end $$;




-- =========================================================
-- ZIPMM MOVIE OWNER SUPER ADMIN PLATFORM (v2)
-- Run the complete file in Supabase SQL Editor as project owner.
-- Owner identity is fixed to the verified email below.
-- Never place a Supabase service_role key in the Android app.
-- =========================================================

create table if not exists public.zipmm_movies (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  category text not null default 'Movies',
  release_year integer,
  rating numeric(3,1),
  poster_url text,
  video_url text,
  description text,
  is_published boolean not null default true,
  is_vip boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists zipmm_movies_category_idx on public.zipmm_movies(category);
create index if not exists zipmm_movies_published_idx on public.zipmm_movies(is_published);

create table if not exists public.zipmm_app_settings (
  setting_key text primary key,
  setting_value jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now(),
  updated_by uuid references auth.users(id)
);

create table if not exists public.zipmm_admin_audit (
  id bigint generated always as identity primary key,
  actor_id uuid references auth.users(id),
  action text not null,
  target_type text,
  target_id text,
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);
create index if not exists zipmm_admin_audit_created_idx on public.zipmm_admin_audit(created_at desc);

alter table public.zipmm_movies enable row level security;
alter table public.zipmm_app_settings enable row level security;
alter table public.zipmm_admin_audit enable row level security;

drop policy if exists "public reads published movies" on public.zipmm_movies;
create policy "public reads published movies" on public.zipmm_movies
for select to anon, authenticated using (is_published = true or public.zipmm_is_admin());

drop policy if exists "admins manage movies" on public.zipmm_movies;
create policy "admins manage movies" on public.zipmm_movies
for all to authenticated using (public.zipmm_is_admin()) with check (public.zipmm_is_admin());

drop policy if exists "admins read settings" on public.zipmm_app_settings;
create policy "admins read settings" on public.zipmm_app_settings
for select to authenticated using (public.zipmm_is_admin());

drop policy if exists "admins read audit" on public.zipmm_admin_audit;
create policy "admins read audit" on public.zipmm_admin_audit
for select to authenticated using (public.zipmm_is_admin());

grant select on public.zipmm_movies to anon, authenticated;
grant insert, update, delete on public.zipmm_movies to authenticated;
grant select on public.zipmm_app_settings, public.zipmm_admin_audit to authenticated;

-- Keep the existing JWT role check, but verify the configured owner independently.
create or replace function public.zipmm_is_owner()
returns boolean language sql stable security definer
set search_path = public, auth
as $$
  select lower(coalesce(auth.jwt() ->> 'email','')) = 'flowerkingmyanmar@gmail.com'
     and coalesce((auth.jwt() -> 'app_metadata' ->> 'role') = 'admin', false);
$$;

create or replace function public.zipmm_admin_audit_write(
  p_action text, p_target_type text default null,
  p_target_id text default null, p_details jsonb default '{}'::jsonb
) returns void language plpgsql security definer
set search_path = public
as $$
begin
  if not public.zipmm_is_admin() then raise exception 'admin_only'; end if;
  insert into public.zipmm_admin_audit(actor_id,action,target_type,target_id,details)
  values(auth.uid(),left(p_action,100),left(p_target_type,60),left(p_target_id,200),coalesce(p_details,'{}'::jsonb));
end;
$$;

-- User listing exposes only safe account fields; no password/hash data.
create or replace function public.zipmm_admin_list_users()
returns table(user_id uuid,email text,created_at timestamptz,last_sign_in_at timestamptz,banned_until timestamptz)
language plpgsql security definer set search_path = public, auth
as $$
begin
  if not public.zipmm_is_admin() then raise exception 'admin_only'; end if;
  return query
    select u.id,u.email::text,u.created_at,u.last_sign_in_at,u.banned_until
    from auth.users u order by u.created_at desc limit 500;
end;
$$;

create or replace function public.zipmm_admin_set_user_ban(p_user_id uuid,p_block boolean)
returns void language plpgsql security definer set search_path = public, auth
as $$
begin
  if not public.zipmm_is_admin() then raise exception 'admin_only'; end if;
  if p_user_id = auth.uid() then raise exception 'cannot_change_own_account'; end if;
  if lower((select email from auth.users where id=p_user_id)) = 'flowerkingmyanmar@gmail.com' then
    raise exception 'cannot_block_owner';
  end if;
  update auth.users set banned_until =
    case when p_block then now() + interval '100 years' else null end
  where id=p_user_id;
  if not found then raise exception 'user_not_found'; end if;
  perform public.zipmm_admin_audit_write(case when p_block then 'user_blocked' else 'user_unblocked' end,'user',p_user_id::text,'{}'::jsonb);
end;
$$;

create or replace function public.zipmm_admin_delete_user(p_user_id uuid)
returns void language plpgsql security definer set search_path = public, auth
as $$
begin
  if not public.zipmm_is_owner() then raise exception 'owner_only'; end if;
  if p_user_id = auth.uid() then raise exception 'cannot_delete_owner'; end if;
  if lower((select email from auth.users where id=p_user_id)) = 'flowerkingmyanmar@gmail.com' then
    raise exception 'cannot_delete_owner';
  end if;
  delete from auth.users where id=p_user_id;
  if not found then raise exception 'user_not_found'; end if;
  perform public.zipmm_admin_audit_write('user_deleted','user',p_user_id::text,'{}'::jsonb);
end;
$$;

-- Only the owner can promote/demote administrators. The owner account is immutable.
create or replace function public.zipmm_owner_set_admin(p_user_id uuid,p_make_admin boolean)
returns void language plpgsql security definer set search_path = public, auth
as $$
begin
  if not public.zipmm_is_owner() then raise exception 'owner_only'; end if;
  if lower((select email from auth.users where id=p_user_id)) = 'flowerkingmyanmar@gmail.com' then
    raise exception 'owner_role_is_fixed';
  end if;
  if p_make_admin then
    update auth.users set raw_app_meta_data =
      coalesce(raw_app_meta_data,'{}'::jsonb) || jsonb_build_object('role','admin')
    where id=p_user_id;
  else
    update auth.users set raw_app_meta_data =
      coalesce(raw_app_meta_data,'{}'::jsonb) - 'role'
    where id=p_user_id;
  end if;
  if not found then raise exception 'user_not_found'; end if;
  perform public.zipmm_admin_audit_write(case when p_make_admin then 'admin_granted' else 'admin_revoked' end,'user',p_user_id::text,'{}'::jsonb);
end;
$$;

create or replace function public.zipmm_admin_save_setting(p_key text,p_value jsonb)
returns void language plpgsql security definer set search_path = public
as $$
begin
  if not public.zipmm_is_owner() then raise exception 'owner_only'; end if;
  insert into public.zipmm_app_settings(setting_key,setting_value,updated_at,updated_by)
  values(left(p_key,100),coalesce(p_value,'{}'::jsonb),now(),auth.uid())
  on conflict(setting_key) do update set setting_value=excluded.setting_value,updated_at=now(),updated_by=auth.uid();
  perform public.zipmm_admin_audit_write('setting_updated','setting',p_key,p_value);
end;
$$;

-- RPC grants: authenticated users may call these, but each function checks role server-side.
revoke all on function public.zipmm_is_owner() from public, anon;
revoke all on function public.zipmm_admin_audit_write(text,text,text,jsonb) from public, anon;
revoke all on function public.zipmm_admin_list_users() from public, anon;
revoke all on function public.zipmm_admin_set_user_ban(uuid,boolean) from public, anon;
revoke all on function public.zipmm_admin_delete_user(uuid) from public, anon;
revoke all on function public.zipmm_owner_set_admin(uuid,boolean) from public, anon;
revoke all on function public.zipmm_admin_save_setting(text,jsonb) from public, anon;
grant execute on function public.zipmm_is_owner() to authenticated;
grant execute on function public.zipmm_admin_list_users() to authenticated;
grant execute on function public.zipmm_admin_set_user_ban(uuid,boolean) to authenticated;
grant execute on function public.zipmm_admin_delete_user(uuid) to authenticated;
grant execute on function public.zipmm_owner_set_admin(uuid,boolean) to authenticated;
grant execute on function public.zipmm_admin_save_setting(text,jsonb) to authenticated;

-- Default operational settings
insert into public.zipmm_app_settings(setting_key,setting_value)
values ('maintenance_mode','false'::jsonb),('registration_enabled','true'::jsonb),('vip_enabled','true'::jsonb)
on conflict(setting_key) do nothing;
