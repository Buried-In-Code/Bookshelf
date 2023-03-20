__all__ = ["router"]

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from pony.orm import db_session

from bookshelf.controllers.book import BookController
from bookshelf.controllers.creator import CreatorController
from bookshelf.controllers.role import RoleController
from bookshelf.database.tables import User
from bookshelf.routers.html.utils import get_token_user, templates

router = APIRouter(prefix="/roles", tags=["Roles"])


@router.get(path="", response_class=HTMLResponse)
def list_roles(
    *,
    request: Request,
    token_user: User | None = Depends(get_token_user),
    name: str = "",
):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        role_list = RoleController.list_roles()
        if name:
            role_list = [
                x
                for x in role_list
                if name.casefold() in x.name.casefold() or x.name.casefold() in name.casefold()
            ]
        return templates.TemplateResponse(
            "list_roles.html",
            {
                "request": request,
                "token_user": token_user.to_model(),
                "role_list": sorted({x.to_model() for x in role_list}),
                "filters": {"name": name},
            },
        )


@router.get(path="/{role_id}", response_class=HTMLResponse)
def view_role(*, request: Request, role_id: int, token_user: User | None = Depends(get_token_user)):
    if not token_user:
        return RedirectResponse("/")
    with db_session:
        role = RoleController.get_role(role_id=role_id)
        creator_dict = {}
        for temp in role.creators:
            temp_creator = temp.creator.to_model()
            if temp_creator not in creator_dict:
                creator_dict[temp_creator] = set()
            creator_dict[temp_creator].add(temp.book.to_model())
        creator_dict = {key: sorted(creator_dict[key]) for key in sorted(creator_dict.keys())}
        return templates.TemplateResponse(
            "view_role.html",
            {
                "request": request,
                "token_user": token_user.to_model(),
                "role": role.to_model(),
                "creators": creator_dict,
                "all_books": sorted({x.to_model() for x in BookController.list_books()}),
                "all_creators": sorted({x.to_model() for x in CreatorController.list_creators()}),
            },
        )


@router.get(path="/{role_id}/edit", response_class=HTMLResponse)
def edit_role(*, request: Request, role_id: int, token_user: User | None = Depends(get_token_user)):
    if not token_user:
        return RedirectResponse("/")
    if token_user.role < 2:
        return RedirectResponse(f"/roles/{role_id}")
    with db_session:
        role = RoleController.get_role(role_id=role_id)
        return templates.TemplateResponse(
            "edit_role.html",
            {
                "request": request,
                "token_user": token_user.to_model(),
                "role": role.to_model(),
            },
        )
