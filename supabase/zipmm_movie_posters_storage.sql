-- ZIPMM MOVIE cover photo storage
-- Run once in Supabase SQL Editor as project owner.
insert into storage.buckets (id,name,public,file_size_limit,allowed_mime_types)
values ('zipmm-movie-posters','zipmm-movie-posters',true,10485760,array['image/jpeg','image/png','image/webp'])
on conflict (id) do update set
 public=true,
 file_size_limit=10485760,
 allowed_mime_types=array['image/jpeg','image/png','image/webp'];

drop policy if exists "ZIPMM owner uploads movie posters" on storage.objects;
create policy "ZIPMM owner uploads movie posters"
on storage.objects for insert to authenticated
with check (bucket_id='zipmm-movie-posters' and public.zipmm_is_owner());

drop policy if exists "ZIPMM owner updates movie posters" on storage.objects;
create policy "ZIPMM owner updates movie posters"
on storage.objects for update to authenticated
using (bucket_id='zipmm-movie-posters' and public.zipmm_is_owner())
with check (bucket_id='zipmm-movie-posters' and public.zipmm_is_owner());

drop policy if exists "ZIPMM owner deletes movie posters" on storage.objects;
create policy "ZIPMM owner deletes movie posters"
on storage.objects for delete to authenticated
using (bucket_id='zipmm-movie-posters' and public.zipmm_is_owner());

-- Public bucket means viewers can load poster URLs; only the owner can upload, replace, or delete.
