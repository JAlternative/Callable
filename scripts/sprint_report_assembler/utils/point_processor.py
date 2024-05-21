def count_points(unfinished_list, completed_list):
    complete_points = count_complete_points(completed_list)
    return count_unfinished_points(unfinished_list, complete_points)


def count_complete_points(completed_list):
    points = 0
    for issue in completed_list:
        if issue.done:
            points = points + issue.done
    return points


def count_unfinished_points(unfinished_list, completed_points):
    unfinished_points = 0
    for issue in unfinished_list:
        assignee = issue.assignee
        if not assignee:
            continue
        if issue.done != issue.wip:
            completed_points = completed_points + issue.done
        unfinished_points = unfinished_points + issue.wip
    return unfinished_points, completed_points
