create or replace function hx_link_history_version() returns trigger as $$
begin

end;
$$ LANGUAGE plpgsql;

create trigger hx_link_history_upd
    before update or delete on hx_link
    for each row execute procedure hx_link_history_version();

create or replace function hi_link_history_version() returns trigger as $$
begin
end;
$$ LANGUAGE plpgsql;
