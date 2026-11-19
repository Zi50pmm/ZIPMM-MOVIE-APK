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
