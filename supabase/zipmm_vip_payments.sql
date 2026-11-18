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

