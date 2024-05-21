class Sprint:
    def __init__(self, json):
        self.id = json["id"]
        self.sequence = json["sequence"]
        self.name = json["name"]
        self.state = json["state"]

    def __str__(self):
        return self.name
