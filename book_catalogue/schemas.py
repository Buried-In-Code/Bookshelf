__all__ = [
    "User",
    "AuthorIdentifiers",
    "Author",
    "Publisher",
    "Series",
    "BookIdentifiers",
    "Book",
]

from pydantic import BaseModel, Field


class User(BaseModel):
    user_id: int
    username: str
    role: int

    def __lt__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, User):
            raise NotImplementedError()
        return self.username < other.username

    def __eq__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, User):
            raise NotImplementedError()
        return self.username == other.username

    def __hash__(self):
        return hash((type(self), self.username))


class AuthorIdentifiers(BaseModel):
    author_id: int
    open_library_id: str | None


class Author(BaseModel):
    name: str
    identifiers: AuthorIdentifiers

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, Author):
            raise NotImplementedError()
        return self.name < other.name

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, Author):
            raise NotImplementedError()
        return self.name == other.name

    def __hash__(self):
        return hash((type(self), self.name))


class Publisher(BaseModel):
    publisher_id: int
    name: str

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, Publisher):
            raise NotImplementedError()
        return self.name < other.name

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, Publisher):
            raise NotImplementedError()
        return self.name == other.name

    def __hash__(self):
        return hash((type(self), self.name))


class Series(BaseModel):
    series_id: int
    title: str
    number: int | None = None

    @property
    def display_name(self) -> str:
        output = self.title
        if self.number:
            return f"{output} (#{self.number})"
        return output

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, Series):
            raise NotImplementedError()
        if self.title != other.title:
            return self.title < other.title
        return (self.number or -1) < (other.number or -1)

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, Series):
            raise NotImplementedError()
        return (self.title, self.number) == (other.title, other.number)

    def __hash__(self):
        return hash((type(self), self.title, self.number))


class BookIdentifiers(BaseModel):
    book_id: int
    isbn_10: str | None = None
    isbn_13: str | None = None
    open_library_id: str | None = None
    google_books_id: str | None = None
    goodreads_id: str | None = None
    library_thing_id: str | None = None


class Book(BaseModel):
    title: str
    subtitle: str | None = None
    authors: list[Author] = Field(default_factory=list)
    format: str | None = None
    series: list[Series] = Field(default_factory=list)
    publishers: list[Publisher] = Field(default_factory=list)
    description: str | None = None
    wisher: User | None = None
    readers: list[User] = Field(default_factory=list)
    identifiers: BookIdentifiers
    image_url: str | None = None

    @property
    def first_author(self) -> Author | None:
        if temp := sorted(self.authors):
            return temp[0]
        return None

    @property
    def first_series(self) -> Series | None:
        if temp := sorted(self.series):
            return temp[0]
        return None

    @property
    def publisher_names(self) -> str:
        return "; ".join(x.name for x in self.publishers)

    @property
    def author_names(self) -> str:
        return "; ".join(x.name for x in self.authors)

    @property
    def series_names(self) -> str:
        return "; ".join(x.title for x in self.series)

    @property
    def reader_names(self) -> str:
        return "; ".join(x.username for x in self.readers)

    def __lt__(self, other) -> int:  # noqa: ANN001
        if not isinstance(other, Book):
            raise NotImplementedError()
        if self.first_series and other.first_series and self.first_series != other.first_series:
            return self.first_series < other.first_series
        if self.first_series and not other.first_series:
            return False
        if not self.first_series and other.first_series:
            return True

        if self.title != other.title:
            return self.title < other.title

        return (self.subtitle or "") < (other.subtitle or "")

    def __eq__(self, other) -> bool:  # noqa: ANN001
        if not isinstance(other, Book):
            raise NotImplementedError()
        return (self.first_series, self.title, (self.subtitle or ""),) == (
            other.first_series,
            other.title,
            (other.subtitle or ""),
        )

    def __hash__(self):
        return hash(
            (
                type(self),
                self.first_series,
                self.title,
                (self.subtitle or ""),
            )
        )
