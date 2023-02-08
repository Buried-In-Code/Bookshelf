__all__ = [
    "Author",
    "Book",
    "BookAuthor",
    "BookSeries",
    "Format",
    "Genre",
    "Publisher",
    "Role",
    "Series",
    "User",
]

from datetime import date
from typing import Optional as Opt

from pony.orm import Database, Optional, PrimaryKey, Required, Set

from book_catalogue.schemas.author import AuthorRead, Identifiers as AuthorIdentifiers
from book_catalogue.schemas.book import BookRead, Identifiers as BookIdentifiers
from book_catalogue.schemas.format import FormatRead
from book_catalogue.schemas.genre import GenreRead
from book_catalogue.schemas.publisher import PublisherRead
from book_catalogue.schemas.role import RoleRead
from book_catalogue.schemas.series import SeriesRead
from book_catalogue.schemas.user import UserRead

db = Database()


class Author(db.Entity):
    _table_ = "Authors"

    author_id: int = PrimaryKey(int, auto=True)
    bio: str | None = Optional(str, nullable=True)
    image_url: str | None = Optional(str, nullable=True)
    name: str = Required(str, unique=True)

    goodreads_id: str | None = Optional(str, nullable=True)
    library_thing_id: str | None = Optional(str, nullable=True)
    open_library_id: str | None = Optional(str, nullable=True, unique=True)

    books: list["BookAuthor"] = Set("BookAuthor")

    def to_schema(self) -> AuthorRead:
        return AuthorRead(
            author_id=self.author_id,
            bio=self.bio,
            identifiers=AuthorIdentifiers(
                goodreads_id=self.goodreads_id,
                library_thing_id=self.library_thing_id,
                open_library_id=self.open_library_id,
            ),
            image_url=self.image_url,
            name=self.name,
            roles=[],
        )


class Book(db.Entity):
    _table_ = "Books"

    authors: list["BookAuthor"] = Set("BookAuthor")
    book_id: int = PrimaryKey(int, auto=True)
    description: str | None = Optional(str, nullable=True)
    format: Opt["Format"] = Optional("Format", nullable=True)
    genres: list["Genre"] = Set("Genre", table="Books_Genres")
    image_url: str = Required(str)
    is_collected: bool = Optional(bool, default=False)
    publish_date: date | None = Optional(date, nullable=True)
    publisher: Opt["Publisher"] = Optional("Publisher", nullable=True)
    readers: list["User"] = Set("User", table="Books_Readers", reverse="read_books")
    series: list["BookSeries"] = Set("BookSeries")
    subtitle: str | None = Optional(str, nullable=True)
    title: str = Required(str)
    wishers: list["User"] = Set("User", table="Books_Wishers", reverse="wished_books")

    goodreads_id: str | None = Optional(str, nullable=True)
    google_books_id: str | None = Optional(str, nullable=True)
    isbn: str = Required(str, unique=True)
    library_thing_id: str | None = Optional(str, nullable=True)
    open_library_id: str | None = Optional(str, nullable=True, unique=True)

    def to_schema(self) -> BookRead:
        return BookRead(
            authors=sorted({x.to_schema() for x in self.authors}),
            book_id=self.book_id,
            description=self.description,
            format=self.format.to_schema() if self.format else None,
            genres=sorted({x.to_schema() for x in self.genres}),
            identifiers=BookIdentifiers(
                goodreads_id=self.goodreads_id,
                google_books_id=self.google_books_id,
                isbn=self.isbn,
                library_thing_id=self.library_thing_id,
                open_library_id=self.open_library_id,
            ),
            image_url=self.image_url,
            is_collected=self.is_collected,
            publish_date=self.publish_date,
            publisher=self.publisher.to_schema() if self.publisher else None,
            readers=sorted({x.to_schema() for x in self.readers}),
            series=sorted({x.to_schema() for x in self.series}),
            subtitle=self.subtitle,
            title=self.title,
            wishers=sorted({x.to_schema() for x in self.wishers}),
        )


class BookAuthor(db.Entity):
    _table_ = "Books_Authors"

    author: Author = Required(Author)
    book: Book = Required(Book)
    roles: list["Role"] = Set("Role", table="Books_Authors_Roles")

    PrimaryKey(book, author)

    def to_schema(self) -> AuthorRead:
        temp = self.author.to_schema()
        temp.roles = sorted({x.to_schema() for x in self.roles})
        return temp


class BookSeries(db.Entity):
    _table_ = "Books_Series"

    book: Book = Required(Book)
    number: int | None = Optional(int, nullable=True)
    series: "Series" = Required("Series")

    PrimaryKey(book, series)

    def to_schema(self) -> SeriesRead:
        temp = self.series.to_schema()
        temp.number = self.number
        return temp


class Format(db.Entity):
    _table_ = "Formats"

    format_id: int = PrimaryKey(int, auto=True)
    name: str = Required(str, unique=True)

    books: list[Book] = Set(Book)

    def to_schema(self) -> FormatRead:
        return FormatRead(format_id=self.format_id, name=self.name)


class Genre(db.Entity):
    _table_ = "Genres"

    genre_id: int = PrimaryKey(int, auto=True)
    name: str = Required(str, unique=True)

    books: list[Book] = Set(Book, table="Books_Genres")

    def to_schema(self) -> GenreRead:
        return GenreRead(genre_id=self.genre_id, name=self.name)


class Publisher(db.Entity):
    _table_ = "Publishers"

    publisher_id: int = PrimaryKey(int, auto=True)
    name: str = Required(str, unique=True)

    books: list[Book] = Set(Book)

    def to_schema(self) -> PublisherRead:
        return PublisherRead(publisher_id=self.publisher_id, name=self.name)


class Role(db.Entity):
    _table_ = "Roles"

    name: str = Required(str, unique=True)
    role_id: int = PrimaryKey(int, auto=True)

    authors: list[BookAuthor] = Set(BookAuthor, table="Books_Authors_Roles")

    def to_schema(self) -> RoleRead:
        return RoleRead(name=self.name, role_id=self.role_id)


class Series(db.Entity):
    series_id: int = PrimaryKey(int, auto=True)
    name: str = Required(str, unique=True)

    books: list[BookSeries] = Set(BookSeries)

    def to_schema(self) -> SeriesRead:
        return SeriesRead(series_id=self.series_id, name=self.name)


class User(db.Entity):
    _table_ = "Users"

    image_url: str | None = Optional(str, nullable=True)
    role: int = Optional(int, default=0)
    user_id: int = PrimaryKey(int, auto=True)
    username: str = Required(str)

    wished_books: list[Book] = Set(Book, table="Books_Wishers", reverse="wishers")
    read_books: list[Book] = Set(Book, table="Books_Readers", reverse="readers")

    def to_schema(self) -> UserRead:
        return UserRead(
            user_id=self.user_id, username=self.username, role=self.role, image_url=self.image_url
        )
