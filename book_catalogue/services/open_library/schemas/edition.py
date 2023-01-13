__all__ = ["Edition"]

from pydantic import Field

from book_catalogue.services.open_library.schemas import (
    BaseModel,
    DatetimeResource,
    Resource,
    TextResource,
)


class Identifiers(BaseModel):
    amazon: list[str] = Field(default_factory=list)
    better_world_books: list[str] = Field(default_factory=list)
    goodreads: list[str] = Field(default_factory=list)
    google: list[str] = Field(default_factory=list)
    librarything: list[str] = Field(default_factory=list)
    wikidata: list[str] = Field(default_factory=list)


class Contributor(BaseModel):
    name: str
    role: str


class Content(BaseModel):
    label: str | None = None
    level: int
    pagenum: str | None
    title: str
    type: Resource | None = None


class Link(BaseModel):
    title: str
    url: str


class Edition(BaseModel):
    authors: list[Resource] = Field(default_factory=list)
    authors: list[Resource] = Field(default_factory=list)
    by_statement: str | None = None
    classifications: dict[int, int]
    contributions: list[str] = Field(default_factory=list)
    contributors: list[Contributor] = Field(default_factory=list)
    copyright_date: str | None = None
    covers: list[int] = Field(default_factory=list)
    created: DatetimeResource
    description: str | TextResource | None = None
    dewey_decimal_class: list[str] = Field(default_factory=list)
    edition_name: str | None = None
    first_sentence: str | None = None
    full_title: str | None = None
    genres: list[str] = Field(default_factory=list)
    ia_box_id: list[str] = Field(default_factory=list)
    ia_loaded_id: list[str] = Field(default_factory=list)
    identifiers: Identifiers
    isbn_10: list[str] = Field(default_factory=list)
    isbn_13: list[str] = Field(default_factory=list)
    key: str
    languages: list[Resource] = Field(default_factory=list)
    last_modified: DatetimeResource
    latest_revision: int
    lc_classifications: list[str] = Field(default_factory=list)
    lccn: list[str] = Field(default_factory=list)
    links: list[Link] = Field(default_factory=list)
    local_id: list[str] = Field(default_factory=list)
    location: list[str] = Field(default_factory=list)
    notes: str | TextResource | None = None
    number_of_pages: int | None = None
    ocaid: str | None = None
    oclc_numbers: list[str] = Field(default_factory=list)
    oclc_number: list[str] = Field(default_factory=list)
    other_titles: list[str] = Field(default_factory=list)
    pagination: str | None = None
    physical_dimensions: str | None = None
    physical_format: str | None = None
    publish_country: str | None = None
    publish_date: str | None = None
    publish_places: list[str] = Field(default_factory=list)
    publishers: list[str]
    revision: int
    series: list[str] = Field(default_factory=list)
    source_records: list[str] = Field(default_factory=list)
    subject_people: list[str] = Field(default_factory=list)
    subject_place: list[str] = Field(default_factory=list)
    subject_places: list[str] = Field(default_factory=list)
    subject_time: list[str] = Field(default_factory=list)
    subjects: list[str] = Field(default_factory=list)
    subtitle: str | None = None
    table_of_contents: list[Content] = Field(default_factory=list)
    title: str
    translated_from: list[Resource] = Field(default_factory=list)
    translation_of: str | None = None
    type: Resource
    weight: str | None = None
    work_title: list[str] = Field(default_factory=list)
    work_titles: list[str] = Field(default_factory=list)
    works: list[Resource]

    @property
    def edition_id(self) -> str:
        return self.key.split("/")[-1]