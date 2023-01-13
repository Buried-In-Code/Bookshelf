__all__ = [
    "create_user",
    "get_user_by_id",
    "get_user_by_username",
    "list_books",
    "add_book_by_isbn",
    "get_book_by_id",
    "get_book_by_isbn",
    "refresh_book",
    "delete_book",
    "get_author_by_id",
    "list_authors",
    "get_series_by_id",
    "list_series",
    "get_publisher_by_id",
    "list_publishers",
]
import logging

from fastapi.exceptions import HTTPException
from pony.orm import flush

from book_catalogue.console import CONSOLE  # noqa: F401
from book_catalogue.database.tables import Author, Book, Publisher, Series, User
from book_catalogue.isbn import to_isbn_13
from book_catalogue.services.open_library import lookup_book
from book_catalogue.services.open_library.service import OpenLibrary

LOGGER = logging.getLogger(__name__)


def create_user(username: str) -> User:
    if User.get(username=username):
        raise HTTPException(status_code=409, detail="User already exists.")
    temp = User(username=username)
    flush()
    return temp


def get_user_by_id(user_id: int) -> User:
    if user := User.get(user_id=user_id):
        return user
    raise HTTPException(status_code=404, detail="User not found.")


def get_user_by_username(username: str) -> User:
    if user := User.get(username=username):
        return user
    raise HTTPException(status_code=404, detail="User not found.")


def list_books() -> list[Book]:
    return Book.select()


def load_open_library() -> OpenLibrary:
    return OpenLibrary(cache=None)


def add_book_by_isbn(isbn: str, wisher: User) -> Book:
    isbn_13 = to_isbn_13(value=isbn)
    if book := Book.get(isbn_13=isbn_13):  # noqa: F841
        raise HTTPException(status_code=409, detail="Book already exists.")
    session = load_open_library()
    if result := lookup_book(session=session, isbn=isbn_13):
        temp = Book(
            authors=[],
            title=result["edition"].title,
            subtitle=result["edition"].subtitle,
            format=result["edition"].physical_format,
            publishers=sorted(
                {
                    Publisher.get(name=y) or Publisher(name=y)
                    for x in result["edition"].publishers
                    for y in x.split(";")
                }
            ),
            series=sorted(
                {
                    Series.get(title=y) or Series(title=y)
                    for x in result["edition"].series
                    for y in x.split(";")
                }
            ),
            wisher=wisher,
            isbn_10=isbn if isbn != isbn_13 else None,
            isbn_13=isbn_13,
            open_library_id=result["edition"].edition_id,
            image_url=f"https://covers.openlibrary.org/b/OLID/{result['edition'].edition_id}-L.jpg",
        )
        flush()
        return temp
    raise HTTPException(status_code=404, detail="Book not found.")


def get_book_by_isbn(isbn: str) -> Book:
    isbn = to_isbn_13(value=isbn)
    if book := Book.get(isbn_13=isbn):
        return book
    raise HTTPException(status_code=404, detail="Book not found.")


def get_book_by_id(book_id: int) -> Book:
    if book := Book.get(book_id=book_id):
        return book
    raise HTTPException(status_code=404, detail="Book not found.")


def refresh_book(book_id: int) -> Book:
    book = get_book_by_id(book_id=book_id)
    session = load_open_library()
    if result := lookup_book(session=session, isbn=book.isbn_13):
        book.title = result["edition"].title
        book.subtitle = result["edition"].subtitle
        book.format = result["edition"].physical_format
        book.publishers = sorted(
            {Publisher.get(name=x) or Publisher(name=x) for x in result["edition"].publisher_list}
        )
        book.open_library_id = result["edition"].edition_id
        flush()
    else:
        pass
    return book


def delete_book(book_id: int) -> None:
    book = get_book_by_id(book_id=book_id)
    book.delete()


def list_authors() -> list[Author]:
    return Author.select()


def get_author_by_id(author_id: int) -> Author:
    if author := Author.get(author_id=author_id):
        return author
    raise HTTPException(status_code=404, detail="Author not found.")


def list_series() -> list[Series]:
    return Series.select()


def get_series_by_id(series_id: int) -> Series:
    if series := Series.get(series_id=series_id):
        return series
    raise HTTPException(status_code=404, detail="Series not found.")


def list_publishers() -> list[Publisher]:
    return Publisher.select()


def get_publisher_by_id(publisher_id: int) -> Publisher:
    if publisher := Publisher.get(publisher_id=publisher_id):
        return publisher
    raise HTTPException(status_code=404, detail="Publisher not found.")