// Class generated with https://app.quicktype.io/ from json response

export class ResultObject {
    isAllTargetsFinal: boolean;
    targets: Target[];

    constructor(isAllTargetsFinal: boolean, targets: Target[]) {
        this.isAllTargetsFinal = isAllTargetsFinal;
        this.targets = targets;
    }

    static fromJson(json: Record<string, any>): ResultObject {
        return new ResultObject(
            json["isAllTargetsFinal"] ?? false,
            Array.isArray(json["targets"])
                ? json["targets"].map((x: any) => Target.fromJson(x))
                : []
        );
    }
}
class Target {
    description: Description;
    isFinal: boolean;
    items: Items;

    constructor(description: Description, isFinal: boolean, items: Items) {
        this.description = description;
        this.isFinal = isFinal;
        this.items = items;
    }

    static fromJson(json: Record<string, any>): Target {
        return new Target(
            Description.fromJson(json["description"] ?? {}),
            json["isFinal"] ?? false,
            Items.fromJson(json["items"] ?? {})
        );
    }
}
class Description {
    targetType: string;
    specificType: string;
    specificTypeHasRfid: number;
    specificTypeDisplayName: string;
    itemTypes: string[];
    attributes: Attributes;

    constructor(
        targetType: string,
        specificType: string,
        specificTypeHasRfid: number,
        specificTypeDisplayName: string,
        itemTypes: string[],
        attributes: Attributes
    ) {
        this.targetType = targetType;
        this.specificType = specificType;
        this.specificTypeHasRfid = specificTypeHasRfid;
        this.specificTypeDisplayName = specificTypeDisplayName;
        this.itemTypes = itemTypes;
        this.attributes = attributes;
    }

    static fromJson(json: Record<string, any>): Description {
        return new Description(
            json["target_type"] ?? "",
            json["specific_type"] ?? "",
            json["specific_type_has_rfid"] ?? -1,
            json["specific_type_display_name"] ?? "",
            Array.isArray(json["item_types"]) ? json["item_types"].map((x: any) => x) : [],
            Attributes.fromJson(json["attributes"] ?? {})
        );
    }
}

class Attributes {
    constructor() {}
    static fromJson(json: Record<string, any>): Attributes {
        return new Attributes();
    }
}

interface Image {
    photo?: Photo;
}

class Photo {
    type: string;
    name: string;
    value: string;
    confidence: number;
    accepted: boolean;
    attributes: Record<string, any>;
    field_geometry: Record<string, any>;
    image_base64: string;

    constructor(
        type: string,
        name: string,
        value: string,
        confidence: number,
        accepted: boolean,
        attributes: Record<string, any>,
        field_geometry: Record<string, any>,
        image_base64: string
    ) {
        this.type = type;
        this.name = name;
        this.value = value;
        this.confidence = confidence;
        this.accepted = accepted;
        this.attributes = attributes;
        this.field_geometry = field_geometry;
        this.image_base64 = image_base64;
    }

    static fromJson(json: Record<string, any>): Photo {
        return new Photo(
            json["type"] ?? "",
            json["name"] ?? "",
            json["value"] ?? "",
            json["confidence"] ?? 0,
            json["accepted"] ?? false,
            json["attributes"] ?? {},
            json["field_geometry"] ?? {},
            json["image_base64"] ?? ""
        );
    }
}

class Items {
    image: Record<string, Photo>;
    string: Record<string, any>;
    template: Record<string, any>;

    constructor(image: Record<string, Photo>, string: Record<string, any>, template: Record<string, any>) {
        this.image = image;
        this.string = string;
        this.template = template;
    }

    static fromJson(json: Record<string, any>): Items {
        const image: Record<string, Photo> = {};

        if (json["image"]) {
            for (const key of Object.keys(json["image"])) {
                image[key] = Photo.fromJson(json["image"][key]);
            }
        }

        return new Items(
            image,
            json["string"] ?? {},
            json["template"] ?? {}
        );
    }
}
