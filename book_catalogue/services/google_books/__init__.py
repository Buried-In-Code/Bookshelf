__all__ = ["lookup_book"]

from fastapi import HTTPException

from book_catalogue.controllers.creator import CreatorController
from book_catalogue.controllers.genre import GenreController
from book_catalogue.controllers.publisher import PublisherController
from book_catalogue.controllers.role import RoleController
from book_catalogue.schemas.book import BookCreatorWrite, BookWrite, Identifiers
from book_catalogue.schemas.creator import CreatorWrite
from book_catalogue.schemas.genre import GenreWrite
from book_catalogue.schemas.publisher import PublisherWrite
from book_catalogue.schemas.role import RoleWrite
from book_catalogue.services.google_books.service import GoogleBooks


def lookup_book(isbn: str, google_books_id: str | None = None) -> BookWrite:
    session = GoogleBooks()
    if google_books_id:
        result = session.get_book(book_id=google_books_id)
    else:
        result = session.get_book_by_isbn(isbn=isbn)
    creators = []
    for _creator in result.volume_info.creators:
        try:
            role = RoleController.get_role_by_name(name="Writer")
        except HTTPException:
            role = RoleController.create_role(new_role=RoleWrite(name="Writer"))
        try:
            creator = CreatorController.get_creator_by_name(name=_creator)
        except HTTPException:
            creator = CreatorController.create_creator(new_creator=CreatorWrite(name=_creator))
        creators.append(BookCreatorWrite(creator_id=creator.creator_id, role_ids=[role.role_id]))

    publisher = None
    if result.volume_info.publisher:
        try:
            publisher = PublisherController.get_publisher_by_name(name=result.volume_info.publisher)
        except HTTPException:
            publisher = PublisherController.create_publisher(
                new_publisher=PublisherWrite(name=result.volume_info.publisher)
            )

    genre_ids = set()
    for _genre in result.volume_info.categories:
        for part in _genre.split("/"):
            try:
                genre = GenreController.get_genre_by_name(name=part.strip())
            except HTTPException:
                genre = GenreController.create_genre(new_genre=GenreWrite(name=part.strip()))
            genre_ids.add(genre.genre_id)

    return BookWrite(
        creators=creators,
        description=result.volume_info.description,
        # TODO: Format Id
        genre_ids=genre_ids,
        identifiers=Identifiers(
            # TODO: Goodreads Id
            google_books_id=result.book_id,
            isbn=isbn,
            # TODO: LibraryThing Id
            # TODO: OpenLibrary Id
        ),
        image_url=f"http://books.google.com/books/content?id={result.book_id}&printsec=frontcover&img=1&zoom=3&edge=curl&source=gbs_api",
        # Is Collected
        publish_date=result.volume_info.published_date,
        publisher_id=publisher.publisher_id if publisher else None,
        # Reader Ids
        # TODO: Series
        subtitle=result.volume_info.subtitle,
        title=result.volume_info.title,
        # Wisher Ids
    )
