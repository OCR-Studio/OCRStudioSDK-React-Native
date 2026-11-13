// Class generated with https://app.quicktype.io/ from json response

export class EngineDescription {
    sessionTypes: string[];
    targetGroups: TargetGroup[];

    constructor(sessionTypes: string[], targetGroups: TargetGroup[]) {
        this.sessionTypes = sessionTypes;
        this.targetGroups = targetGroups;
    }

    static fromJson(json: { [key: string]: any }): EngineDescription {
        return new EngineDescription(
            json["session_types"].map((x: any) => x),
            json["target_groups"].map((x: any) => TargetGroup.fromJson(x))
        );
    }

    toJson(): { [key: string]: any } {
        return {
            "session_types": this.sessionTypes.map((x) => x),
            "target_groups": this.targetGroups.map((x) => x.toJson())
        };
    }
}

class TargetGroup {
    targetGroupType: string;
    targets: string[];
    targetMasks: string[];

    constructor(targetGroupType: string, targets: string[], targetMasks: string[]) {
        this.targetGroupType = targetGroupType;
        this.targets = targets;
        this.targetMasks = targetMasks;
    }

    static fromJson(json: { [key: string]: any }): TargetGroup {
        return new TargetGroup(
            json["target_group_type"],
            json["targets"].map((x: any) => x),
            json["target_masks"].map((x: any) => x)
        );
    }

    toJson(): { [key: string]: any } {
        return {
            "target_group_type": this.targetGroupType,
            "targets": this.targets.map((x) => x),
            "target_masks": this.targetMasks.map((x) => x)
        };
    }
}