#include "List.h"
#include "Utils.h"
using namespace NsBabysitter;

List::List()
  : mNumNodes(0), mFirstNode(NULL)
{

}

List::~List()
{
    removeAllAndDelete();
}

ListNode*
List::getLast()const
{
    if (isEmpty()) {
        return NULL;
    }
    ListNode* nextNode = mFirstNode;
    while (NULL != nextNode->getPtrNext()) {
        nextNode = nextNode->getPtrNext();
    }
   return nextNode;
}

// This method must ONLY ever return false if the newNode is NULL.  This is
// so callers can pass in allocated memory without checking it for NULL, and
// use the return value of this method to check for allocation issues.  This
// makes the interface much simpler to use.
bool
List::insertAfter(ListNode* currNode, ListNode* newNode)
{
    if (!newNode) {
        MYLOGE("insertAfter: newNode is NULL!");
        return false;
    }

    if (currNode && !mFirstNode) {
        // This indicates that the user of the list is confused.  There can't
        // be a currNode when the list is empty.
        PANIC("insertAfter: Can't insert relative to currNode on empty list");
    }

    if (!mFirstNode) {
        // currNode must also be NULL per the check above.
        // The list is empty, so add the new node as the first and only in the list.
        mFirstNode = newNode;
        mFirstNode->setPtrPrev(NULL);
        mFirstNode->setPtrNext(NULL);
    } else {
        if (!currNode) {
            // We assume here that the user wants to add to the end of the list,
            // but was too shy to call insertLast().
            currNode = getLast();
            if (!currNode) {
                // We already checked for mFirstNode for NULL above, so this
                // check is useless except to satisfy Klocworks, which is
                // complaining that getLast() might return NULL, which can't
                // happen.
                PANIC("insertAfter: The impossible has happened!");
            }
        }
        // Update the new node first with information from the current and
        // previous nodes.
        ListNode* nextNode = currNode->getPtrNext();
        newNode->setPtrPrev(currNode);
        newNode->setPtrNext(nextNode);
        // Update the current node.
        currNode->setPtrNext(newNode);
        // Update the next node, unless it's NULL, which implies the current
        // node was the last in the list.
        if (nextNode) {
            nextNode->setPtrPrev(newNode);
        }
    }
    mNumNodes++;
    return true;
}

// This method must ONLY ever return false if the newNode is NULL.  This is
// so callers can pass in allocated memory without checking it for NULL, and
// use the return value of this method to check for allocation issues.  This
// makes the interface much simpler to use.
bool
List::insertBefore(ListNode* currNode, ListNode* newNode)
{
    // Do sanity checks first with inline return statements.
    if (!newNode) {
        MYLOGE("insertBefore: newNode is NULL!");
        return false;
    }

    if (currNode && !mFirstNode) {
        // This indicates that the user of the list is confused.  There can't
        // be a currNode when the list is empty.
        PANIC("insertAfter: Can't insert relative to currNode on empty list");
    }

    if (!mFirstNode) {
        // currNode must also be NULL per the check above.
        // The list is empty, so add the new node as the first and only in the
        // list.
        mFirstNode = newNode;
        mFirstNode->setPtrPrev(NULL);
        mFirstNode->setPtrNext(NULL);
    } else {
        if (!currNode) {
            // We assume here that the user wants to add to the beginning of
            // the list, but was too shy to call insertFirst().
            currNode = mFirstNode;
        }
        // Update the new node first with information from the current and
        // previous nodes.
        ListNode* prevNode = currNode->getPtrPrev();
        newNode->setPtrPrev(prevNode);
        newNode->setPtrNext(currNode);
        // Update the current node.
        currNode->setPtrPrev(newNode);
        // Update the previous node.
        if (!prevNode) {
            // This implies that we just inserted before the first node in the
            // list, so update the pointer.
            mFirstNode = newNode;
        } else {
            prevNode->setPtrNext(newNode);
        }
    }
    mNumNodes++;
    return true;
}

ListNode*
List::dequeue()
{
    ListNode* lastNode = getLast();
    if (lastNode) {
        remove(lastNode);
    }
    return lastNode;
}

void
List::remove(ListNode* node)
{
    if (!node) {
        MYLOGE("remove: node is NULL!");
        return;
    }

    ListNode* prevNode = node->getPtrPrev();
    ListNode* nextNode = node->getPtrNext();
    // Update the previous node if it exists.  If it doesn't, then we just
    // removed the first node in the list.
    if (!prevNode) {
        // This implies that we are removing the first node in the list, so
        // update the pointer.
        mFirstNode = nextNode;
    } else {
        prevNode->setPtrNext(nextNode);
    }

    if (nextNode) {
        // Update the next node if it exists.
        nextNode->setPtrPrev(prevNode);
    }
    mNumNodes--;
}


void
List::removeAllAndDelete()
{
    ListNode* node = mFirstNode;
    while (node) {
        ListNode* nextNode = node->getPtrNext();
        removeAndDelete(node);
        node = nextNode;
    }
}


void
List::removeAndDelete(ListNode* node)
{
    if (!node) {
        MYLOGE("removeAndDelete: node is NULL!");
        return;
    }

    remove(node);
    delete node;
}
