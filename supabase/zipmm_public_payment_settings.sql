-- Allow the app to display only public payment instructions to non-admin users.
drop policy if exists "public reads payment instructions" on public.zipmm_app_settings;
create policy "public reads payment instructions"
on public.zipmm_app_settings for select to anon, authenticated
using (setting_key in ('kbzpay_account','wavepay_account'));
grant select on public.zipmm_app_settings to anon, authenticated;
