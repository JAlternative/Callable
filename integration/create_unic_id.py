import random
import string


def create_unic_id():
    unic_id = ''.join([random.choice([random.choice(string.ascii_letters), str(random.randint(0, 9))]) for _ in range(5)])
    return unic_id